package org.sagebionetworks.template.repo.search;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.opensearch.client.opensearch.generic.Body;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.generic.Response;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.OpenSearchClientFactory;
import org.sagebionetworks.template.ThreadProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;

/**
 * Registers the embedding model that SearchIndex semantic search queries against on the stack's
 * OpenSearch domain: a connector that signs Bedrock {@code InvokeModel} calls, a model group to own
 * the model, and the deployed remote model itself.
 *
 * <p>Creating the connector requires {@code iam:PassRole} on the role it names, which the identity
 * that provisions the stack holds and a developer's SSO session does not. Registering here keeps the
 * grant on the provisioning identity: a locally running repository finds the model this step left
 * behind rather than needing to create it.</p>
 *
 * <p>Every resource is found by name before it is created, so re-running a stack build is a no-op
 * and a partially completed earlier run resumes where it stopped.</p>
 */
public class SemanticEmbeddingBuilder {

	/**
	 * Bedrock foundation model and output vector width behind every stored or queried vector. Both are
	 * read back off the connector by the repository, which fails a build or query whose index was
	 * embedded under a different pair, so changing either one here re-embeds rather than corrupts.
	 */
	static final String FOUNDATION_MODEL = "amazon.titan-embed-text-v2:0";
	static final int DIMENSION = 1024;

	static final String CONNECTOR_NAME = "synapse-semantic-embedding-bedrock";
	static final String MODEL_GROUP_NAME = "synapse-semantic-embedding";
	static final String MODEL_NAME = "synapse-semantic-embedding";

	static final String OUTPUT_DOMAIN_ENDPOINT = "SynapseSearchIndexDomainEndpoint";
	static final String OUTPUT_BEDROCK_EMBED_ROLE_ARN = "SynapseSearchIndexBedrockEmbedRoleArn";

	static final String CONNECTOR_TEMPLATE = "templates/repo/search/semantic-embedding-connector.json.vpt";

	private static final Region REGION = Region.US_EAST_1;
	private static final int HTTP_NOT_FOUND = 404;

	// A remote model reaches DEPLOYED within seconds; the ceiling only bounds a domain that is wedged.
	static final int DEPLOY_POLL_ATTEMPTS = 30;
	static final long DEPLOY_POLL_INTERVAL_MS = 4000L;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final Logger logger;
	private final OpenSearchClientFactory clientFactory;
	private final ThreadProvider threadProvider;
	private final VelocityEngine velocityEngine;

	@Inject
	public SemanticEmbeddingBuilder(LoggerFactory loggerFactory, OpenSearchClientFactory clientFactory,
			ThreadProvider threadProvider, VelocityEngine velocityEngine) {
		this.logger = loggerFactory.getLogger(SemanticEmbeddingBuilder.class);
		this.clientFactory = clientFactory;
		this.threadProvider = threadProvider;
		this.velocityEngine = velocityEngine;
	}

	/**
	 * @param sharedResources the completed shared resources stack, whose outputs name the domain
	 *                        endpoint and the role the connector hands to Bedrock.
	 */
	public void buildSemanticEmbedding(Stack sharedResources) throws InterruptedException {
		String host = requiredOutput(sharedResources, OUTPUT_DOMAIN_ENDPOINT);
		String bedrockRoleArn = requiredOutput(sharedResources, OUTPUT_BEDROCK_EMBED_ROLE_ARN);
		OpenSearchGenericClient client = clientFactory.getDomainGenericClient(host);

		Optional<String> deployed = findNewestDeployedModel(client);
		if (deployed.isPresent()) {
			logger.info("Semantic embedding model {} is already deployed on {}.", deployed.get(), host);
			return;
		}

		String modelGroupId = findOrCreateModelGroup(client);
		String connectorId = findOrCreateConnector(client, bedrockRoleArn);
		// model_group_id is always explicit: registering without one silently auto-creates a second
		// model group alongside the one this step owns.
		String modelId = requiredId(post(client, "/_plugins/_ml/models/_register", String.format(
				"{\"name\":\"%s\",\"function_name\":\"remote\",\"model_group_id\":\"%s\",\"connector_id\":\"%s\","
						+ "\"description\":\"Synapse semantic search embeddings (%s/%d).\"}",
				MODEL_NAME, modelGroupId, connectorId, FOUNDATION_MODEL, DIMENSION)), "model_id");
		post(client, "/_plugins/_ml/models/" + modelId + "/_deploy", "{}");

		awaitDeployed(client, modelId);
		logger.info("Registered semantic embedding model {} on {}.", modelId, host);
	}

	private static String requiredOutput(Stack stack, String outputKey) {
		return stack.outputs().stream().filter(output -> outputKey.equals(output.outputKey()))
				.map(Output::outputValue).findFirst()
				.orElseThrow(() -> new IllegalStateException("Failed to find shared resources output: " + outputKey));
	}

	/**
	 * Blocks until the newly registered model can answer a predict call, so the repository instances
	 * started later in this build find it rather than failing until their next scheduled reconcile.
	 */
	private void awaitDeployed(OpenSearchGenericClient client, String modelId) throws InterruptedException {
		for (int attempt = 0; attempt < DEPLOY_POLL_ATTEMPTS; attempt++) {
			String state = post(client, "/_plugins/_ml/models/_search",
					"{\"size\":1,\"query\":{\"ids\":{\"values\":[\"" + modelId + "\"]}}}")
							.path("hits").path("hits").path(0).path("_source").path("model_state").asText();
			if ("DEPLOYED".equals(state)) {
				return;
			}
			logger.info("Waiting for model {} to deploy, state: {}", modelId, state);
			threadProvider.sleep(DEPLOY_POLL_INTERVAL_MS);
		}
		throw new IllegalStateException("Semantic embedding model " + modelId + " did not reach DEPLOYED.");
	}

	private Optional<String> findNewestDeployedModel(OpenSearchGenericClient client) {
		// Only a DEPLOYED model can answer a predict call, and the newest one wins so a duplicate left
		// behind by an earlier run does not shadow the current registration.
		return firstHitId(client, "/_plugins/_ml/models/_search", String.format(
				"{\"size\":1,\"query\":{\"bool\":{\"filter\":[{\"term\":{\"name.keyword\":\"%s\"}},"
						+ "{\"term\":{\"model_state\":\"DEPLOYED\"}}]}},"
						+ "\"sort\":[{\"created_time\":{\"order\":\"desc\"}}]}",
				MODEL_NAME));
	}

	private String findOrCreateModelGroup(OpenSearchGenericClient client) {
		return firstHitId(client, "/_plugins/_ml/model_groups/_search", searchByName(MODEL_GROUP_NAME))
				.orElseGet(() -> requiredId(post(client, "/_plugins/_ml/model_groups/_register", String.format(
						"{\"name\":\"%s\",\"description\":\"Synapse semantic search embedding models.\"}",
						MODEL_GROUP_NAME)), "model_group_id"));
	}

	private String findOrCreateConnector(OpenSearchGenericClient client, String bedrockRoleArn) {
		return firstHitId(client, "/_plugins/_ml/connectors/_search", searchByName(CONNECTOR_NAME))
				.orElseGet(() -> requiredId(
						post(client, "/_plugins/_ml/connectors/_create", connectorBody(bedrockRoleArn)),
						"connector_id"));
	}

	/**
	 * The Bedrock Titan text-embedding connector blueprint. {@code dimensions} and {@code normalize}
	 * are the model's own defaults but are sent explicitly, so a change to those defaults cannot
	 * silently invalidate already-built indexes. {@code client_config} retries a Bedrock 429/5xx
	 * instead of the ML-Commons default of failing the bulk item outright.
	 */
	private String connectorBody(String bedrockRoleArn) {
		VelocityContext context = new VelocityContext();
		context.put("connectorName", CONNECTOR_NAME);
		context.put("region", REGION.id());
		context.put("foundationModel", FOUNDATION_MODEL);
		context.put("dimension", DIMENSION);
		context.put("bedrockRoleArn", bedrockRoleArn);
		StringWriter writer = new StringWriter();
		velocityEngine.getTemplate(CONNECTOR_TEMPLATE).merge(context, writer);
		try {
			return MAPPER.readTree(writer.toString()).toString();
		} catch (IOException e) {
			throw new IllegalStateException("Connector template is not valid JSON", e);
		}
	}

	private static String searchByName(String name) {
		return String.format("{\"size\":1,\"query\":{\"term\":{\"name.keyword\":\"%s\"}}}", name);
	}

	private Optional<String> firstHitId(OpenSearchGenericClient client, String endpoint, String requestBody) {
		JsonNode hits = post(client, endpoint, requestBody).path("hits").path("hits");
		return hits.isEmpty() ? Optional.empty() : Optional.of(hits.get(0).path("_id").asText());
	}

	private static String requiredId(JsonNode response, String field) {
		JsonNode id = response.path(field);
		if (!id.isTextual() || id.asText().trim().isEmpty()) {
			throw new IllegalStateException("ML-Commons response carries no '" + field + "': " + response);
		}
		return id.asText();
	}

	private static JsonNode post(OpenSearchGenericClient client, String endpoint, String requestBody) {
		try (Response response = client
				.execute(Requests.builder().method("POST").endpoint(endpoint).json(requestBody).build())) {
			String responseBody = response.getBody().map(Body::bodyAsString).orElse("");
			// A domain that has never held this kind of ML resource has no backing system index, so a
			// search 404s rather than returning zero hits.
			if (response.getStatus() == HTTP_NOT_FOUND) {
				return MAPPER.createObjectNode();
			}
			if (response.getStatus() >= 300) {
				throw new IllegalStateException("ML-Commons request to " + endpoint + " failed with "
						+ response.getStatus() + ": " + responseBody);
			}
			return MAPPER.readTree(responseBody);
		} catch (IOException e) {
			throw new IllegalStateException("Failed ML-Commons request to " + endpoint, e);
		}
	}
}
