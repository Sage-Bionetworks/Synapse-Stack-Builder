package org.sagebionetworks.template.repo.search;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.ThreadProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

/**
 * Registers the embedding model that SearchIndex semantic search queries against on the stack's
 * OpenSearch domain: a connector that signs Bedrock {@code InvokeModel} calls, a model group to own
 * the model, and the deployed remote model itself.
 *
 * <p>Creating the connector requires {@code iam:PassRole} on the role it names, which a developer's
 * SSO session does not hold. Registering here keeps the grant off every runtime identity: a locally
 * running repository finds the model this step left behind rather than needing to create it.</p>
 *
 * <p>The build host runs outside the stack VPC and cannot reach a VPC-attached domain, so every
 * ML-Commons call goes through the stack's proxy function, which runs inside the domain's subnets,
 * signs with a role that holds the {@code iam:PassRole} grant, and returns the HTTP status and body
 * unchanged.</p>
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
	static final String OUTPUT_ML_PROXY_FUNCTION_NAME = "SynapseSearchIndexMlProxyFunctionName";

	static final String CONNECTOR_TEMPLATE = "templates/repo/search/semantic-embedding-connector.json.vpt";

	private static final Region REGION = Region.US_EAST_1;
	private static final int HTTP_NOT_FOUND = 404;

	// A remote model reaches DEPLOYED within seconds; the ceiling only bounds a domain that is wedged.
	static final int DEPLOY_POLL_ATTEMPTS = 30;
	static final long DEPLOY_POLL_INTERVAL_MS = 4000L;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final Logger logger;
	private final LambdaClient lambdaClient;
	private final ThreadProvider threadProvider;
	private final VelocityEngine velocityEngine;

	@Inject
	public SemanticEmbeddingBuilder(LoggerFactory loggerFactory, LambdaClient lambdaClient,
			ThreadProvider threadProvider, VelocityEngine velocityEngine) {
		this.logger = loggerFactory.getLogger(SemanticEmbeddingBuilder.class);
		this.lambdaClient = lambdaClient;
		this.threadProvider = threadProvider;
		this.velocityEngine = velocityEngine;
	}

	/**
	 * @param sharedResources the completed shared resources stack, whose outputs name the domain
	 *                        endpoint, the role the connector hands to Bedrock, and the proxy
	 *                        function that reaches the domain.
	 */
	public void buildSemanticEmbedding(Stack sharedResources) throws InterruptedException {
		String host = requiredOutput(sharedResources, OUTPUT_DOMAIN_ENDPOINT);
		String bedrockRoleArn = requiredOutput(sharedResources, OUTPUT_BEDROCK_EMBED_ROLE_ARN);
		String proxyFunction = requiredOutput(sharedResources, OUTPUT_ML_PROXY_FUNCTION_NAME);

		Optional<String> deployed = findNewestDeployedModel(proxyFunction);
		if (deployed.isPresent()) {
			logger.info("Semantic embedding model {} is already deployed on {}.", deployed.get(), host);
			return;
		}

		String modelGroupId = findOrCreateModelGroup(proxyFunction);
		String connectorId = findOrCreateConnector(proxyFunction, bedrockRoleArn);
		// model_group_id is always explicit: registering without one silently auto-creates a second
		// model group alongside the one this step owns.
		String modelId = requiredId(post(proxyFunction, "/_plugins/_ml/models/_register", String.format(
				"{\"name\":\"%s\",\"function_name\":\"remote\",\"model_group_id\":\"%s\",\"connector_id\":\"%s\","
						+ "\"description\":\"Synapse semantic search embeddings (%s/%d).\"}",
				MODEL_NAME, modelGroupId, connectorId, FOUNDATION_MODEL, DIMENSION)), "model_id");
		post(proxyFunction, "/_plugins/_ml/models/" + modelId + "/_deploy", "{}");

		awaitDeployed(proxyFunction, modelId);
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
	private void awaitDeployed(String proxyFunction, String modelId) throws InterruptedException {
		for (int attempt = 0; attempt < DEPLOY_POLL_ATTEMPTS; attempt++) {
			String state = post(proxyFunction, "/_plugins/_ml/models/_search",
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

	private Optional<String> findNewestDeployedModel(String proxyFunction) {
		// Only a DEPLOYED model can answer a predict call, and the newest one wins so a duplicate left
		// behind by an earlier run does not shadow the current registration.
		return firstHitId(proxyFunction, "/_plugins/_ml/models/_search", String.format(
				"{\"size\":1,\"query\":{\"bool\":{\"filter\":[{\"term\":{\"name.keyword\":\"%s\"}},"
						+ "{\"term\":{\"model_state\":\"DEPLOYED\"}}]}},"
						+ "\"sort\":[{\"created_time\":{\"order\":\"desc\"}}]}",
				MODEL_NAME));
	}

	private String findOrCreateModelGroup(String proxyFunction) {
		return firstHitId(proxyFunction, "/_plugins/_ml/model_groups/_search", searchByName(MODEL_GROUP_NAME))
				.orElseGet(() -> requiredId(post(proxyFunction, "/_plugins/_ml/model_groups/_register", String.format(
						"{\"name\":\"%s\",\"description\":\"Synapse semantic search embedding models.\"}",
						MODEL_GROUP_NAME)), "model_group_id"));
	}

	private String findOrCreateConnector(String proxyFunction, String bedrockRoleArn) {
		return firstHitId(proxyFunction, "/_plugins/_ml/connectors/_search", searchByName(CONNECTOR_NAME))
				.orElseGet(() -> requiredId(
						post(proxyFunction, "/_plugins/_ml/connectors/_create", connectorBody(bedrockRoleArn)),
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

	private Optional<String> firstHitId(String proxyFunction, String endpoint, String requestBody) {
		JsonNode hits = post(proxyFunction, endpoint, requestBody).path("hits").path("hits");
		return hits.isEmpty() ? Optional.empty() : Optional.of(hits.get(0).path("_id").asText());
	}

	private static String requiredId(JsonNode response, String field) {
		JsonNode id = response.path(field);
		if (!id.isTextual() || id.asText().trim().isEmpty()) {
			throw new IllegalStateException("ML-Commons response carries no '" + field + "': " + response);
		}
		return id.asText();
	}

	private JsonNode post(String proxyFunction, String endpoint, String requestBody) {
		String payload = MAPPER.createObjectNode().put("path", endpoint).put("body", requestBody).toString();
		InvokeResponse invoke = lambdaClient.invoke(InvokeRequest.builder().functionName(proxyFunction)
				.invocationType(InvocationType.REQUEST_RESPONSE).payload(SdkBytes.fromUtf8String(payload)).build());
		String invokeBody = invoke.payload().asUtf8String();
		// The proxy raises only when it cannot complete the request; every HTTP status comes back as data.
		if (invoke.functionError() != null) {
			throw new IllegalStateException("Failed ML-Commons request to " + endpoint + ": " + invokeBody);
		}
		try {
			JsonNode response = MAPPER.readTree(invokeBody);
			int status = response.path("status").asInt();
			String responseBody = response.path("body").asText();
			// A domain that has never held this kind of ML resource has no backing system index, so a
			// search 404s rather than returning zero hits.
			if (status == HTTP_NOT_FOUND) {
				return MAPPER.createObjectNode();
			}
			if (status >= 300) {
				throw new IllegalStateException("ML-Commons request to " + endpoint + " failed with "
						+ status + ": " + responseBody);
			}
			return MAPPER.readTree(responseBody);
		} catch (IOException e) {
			throw new IllegalStateException("Failed ML-Commons request to " + endpoint, e);
		}
	}
}
