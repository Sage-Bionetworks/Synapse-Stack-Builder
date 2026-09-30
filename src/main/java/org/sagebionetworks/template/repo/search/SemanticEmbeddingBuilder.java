package org.sagebionetworks.template.repo.search;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.ThreadProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
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

	private static final String SIGNING_SERVICE = "es";
	private static final Region REGION = Region.US_EAST_1;
	private static final int HTTP_NOT_FOUND = 404;

	// A remote model reaches DEPLOYED within seconds; the ceiling only bounds a domain that is wedged.
	static final int DEPLOY_POLL_ATTEMPTS = 30;
	static final long DEPLOY_POLL_INTERVAL_MS = 4000L;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final Logger logger;
	private final SdkHttpClient httpClient;
	private final AwsCredentialsProvider credentialsProvider;
	private final ThreadProvider threadProvider;

	@Inject
	public SemanticEmbeddingBuilder(LoggerFactory loggerFactory, SdkHttpClient httpClient,
			AwsCredentialsProvider credentialsProvider, ThreadProvider threadProvider) {
		this.logger = loggerFactory.getLogger(SemanticEmbeddingBuilder.class);
		this.httpClient = httpClient;
		this.credentialsProvider = credentialsProvider;
		this.threadProvider = threadProvider;
	}

	/**
	 * @param sharedResources the completed shared resources stack, whose outputs name the domain
	 *                        endpoint and the role the connector hands to Bedrock.
	 */
	public void buildSemanticEmbedding(Stack sharedResources) throws InterruptedException {
		String host = requiredOutput(sharedResources, OUTPUT_DOMAIN_ENDPOINT);
		String bedrockRoleArn = requiredOutput(sharedResources, OUTPUT_BEDROCK_EMBED_ROLE_ARN);

		Optional<String> deployed = findNewestDeployedModel(host);
		if (deployed.isPresent()) {
			logger.info("Semantic embedding model {} is already deployed on {}.", deployed.get(), host);
			return;
		}

		String modelGroupId = findOrCreateModelGroup(host);
		String connectorId = findOrCreateConnector(host, bedrockRoleArn);
		// model_group_id is always explicit: registering without one silently auto-creates a second
		// model group alongside the one this step owns.
		String modelId = requiredId(post(host, "/_plugins/_ml/models/_register", String.format(
				"{\"name\":\"%s\",\"function_name\":\"remote\",\"model_group_id\":\"%s\",\"connector_id\":\"%s\","
						+ "\"description\":\"Synapse semantic search embeddings (%s/%d).\"}",
				MODEL_NAME, modelGroupId, connectorId, FOUNDATION_MODEL, DIMENSION)), "model_id");
		post(host, "/_plugins/_ml/models/" + modelId + "/_deploy", "{}");

		awaitDeployed(host, modelId);
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
	private void awaitDeployed(String host, String modelId) throws InterruptedException {
		for (int attempt = 0; attempt < DEPLOY_POLL_ATTEMPTS; attempt++) {
			String state = post(host, "/_plugins/_ml/models/_search",
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

	private Optional<String> findNewestDeployedModel(String host) {
		// Only a DEPLOYED model can answer a predict call, and the newest one wins so a duplicate left
		// behind by an earlier run does not shadow the current registration.
		return firstHitId(host, "/_plugins/_ml/models/_search", String.format(
				"{\"size\":1,\"query\":{\"bool\":{\"filter\":[{\"term\":{\"name.keyword\":\"%s\"}},"
						+ "{\"term\":{\"model_state\":\"DEPLOYED\"}}]}},"
						+ "\"sort\":[{\"created_time\":{\"order\":\"desc\"}}]}",
				MODEL_NAME));
	}

	private String findOrCreateModelGroup(String host) {
		return firstHitId(host, "/_plugins/_ml/model_groups/_search", searchByName(MODEL_GROUP_NAME))
				.orElseGet(() -> requiredId(post(host, "/_plugins/_ml/model_groups/_register", String.format(
						"{\"name\":\"%s\",\"description\":\"Synapse semantic search embedding models.\"}",
						MODEL_GROUP_NAME)), "model_group_id"));
	}

	private String findOrCreateConnector(String host, String bedrockRoleArn) {
		return firstHitId(host, "/_plugins/_ml/connectors/_search", searchByName(CONNECTOR_NAME))
				.orElseGet(() -> requiredId(
						post(host, "/_plugins/_ml/connectors/_create", connectorBody(bedrockRoleArn)),
						"connector_id"));
	}

	/**
	 * The Bedrock Titan text-embedding connector blueprint. {@code dimensions} and {@code normalize}
	 * are the model's own defaults but are sent explicitly, so a change to those defaults cannot
	 * silently invalidate already-built indexes. {@code client_config} retries a Bedrock 429/5xx
	 * instead of the ML-Commons default of failing the bulk item outright.
	 */
	private String connectorBody(String bedrockRoleArn) {
		return String.format("{\"name\":\"%s\","
				+ "\"description\":\"Synapse semantic search embeddings via Amazon Bedrock.\","
				+ "\"version\":1,\"protocol\":\"aws_sigv4\","
				+ "\"parameters\":{\"region\":\"%s\",\"service_name\":\"bedrock\",\"model\":\"%s\","
				+ "\"dimensions\":%d,\"normalize\":true,\"embeddingTypes\":[\"float\"]},"
				+ "\"credential\":{\"roleArn\":\"%s\"},"
				+ "\"client_config\":{\"max_retry_times\":4,\"retry_backoff_policy\":\"exponential_full_jitter\"},"
				+ "\"actions\":[{\"action_type\":\"predict\",\"method\":\"POST\","
				+ "\"url\":\"https://bedrock-runtime.%s.amazonaws.com/model/%s/invoke\","
				+ "\"headers\":{\"content-type\":\"application/json\",\"x-amz-content-sha256\":\"required\"},"
				+ "\"request_body\":\"{ \\\"inputText\\\": \\\"${parameters.inputText}\\\", "
				+ "\\\"dimensions\\\": ${parameters.dimensions}, "
				+ "\\\"normalize\\\": ${parameters.normalize}, "
				+ "\\\"embeddingTypes\\\": ${parameters.embeddingTypes} }\","
				+ "\"pre_process_function\":\"connector.pre_process.bedrock.embedding\","
				+ "\"post_process_function\":\"connector.post_process.bedrock.embedding\"}]}",
				CONNECTOR_NAME, REGION.id(), FOUNDATION_MODEL, DIMENSION, bedrockRoleArn, REGION.id(),
				FOUNDATION_MODEL);
	}

	private static String searchByName(String name) {
		return String.format("{\"size\":1,\"query\":{\"term\":{\"name.keyword\":\"%s\"}}}", name);
	}

	private Optional<String> firstHitId(String host, String endpoint, String requestBody) {
		JsonNode hits = post(host, endpoint, requestBody).path("hits").path("hits");
		return hits.isEmpty() ? Optional.empty() : Optional.of(hits.get(0).path("_id").asText());
	}

	private static String requiredId(JsonNode response, String field) {
		JsonNode id = response.path(field);
		if (!id.isTextual() || id.asText().trim().isEmpty()) {
			throw new IllegalStateException("ML-Commons response carries no '" + field + "': " + response);
		}
		return id.asText();
	}

	/**
	 * SigV4-sign and POST to an ML-Commons endpoint. The plugin's REST API has no AWS SDK client and no
	 * CloudFormation resource, so the request is signed by hand for the {@code es} service.
	 */
	JsonNode post(String host, String endpoint, String requestBody) {
		byte[] body = requestBody.getBytes(StandardCharsets.UTF_8);
		SdkHttpFullRequest request = SdkHttpFullRequest.builder()
				.method(SdkHttpMethod.POST)
				.uri(URI.create("https://" + host + endpoint))
				.putHeader("Content-Type", "application/json")
				.putHeader("Content-Length", String.valueOf(body.length))
				.contentStreamProvider(() -> new ByteArrayInputStream(body))
				.build();

		SignedRequest signed = AwsV4HttpSigner.create().sign(req -> req
				.identity(credentialsProvider.resolveCredentials())
				.request(request)
				.payload(request.contentStreamProvider().orElseThrow())
				.putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, SIGNING_SERVICE)
				.putProperty(AwsV4HttpSigner.REGION_NAME, REGION.id()));

		try {
			HttpExecuteResponse response = httpClient.prepareRequest(HttpExecuteRequest.builder()
					.request(signed.request())
					.contentStreamProvider(signed.payload().orElse(null))
					.build()).call();

			String responseBody = readBody(response);
			int status = response.httpResponse().statusCode();
			// A domain that has never held this kind of ML resource has no backing system index, so a
			// search 404s rather than returning zero hits.
			if (status == HTTP_NOT_FOUND) {
				return MAPPER.createObjectNode();
			}
			if (!response.httpResponse().isSuccessful()) {
				throw new IllegalStateException(
						"ML-Commons request to " + endpoint + " failed with " + status + ": " + responseBody);
			}
			return MAPPER.readTree(responseBody);
		} catch (IOException e) {
			throw new IllegalStateException("Failed ML-Commons request to " + endpoint, e);
		}
	}

	private static String readBody(HttpExecuteResponse response) throws IOException {
		Optional<InputStream> stream = response.responseBody().map(body -> (InputStream) body);
		if (!stream.isPresent()) {
			return "";
		}
		try (InputStream in = stream.get()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
