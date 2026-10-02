package org.sagebionetworks.template.repo.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.generic.Body;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Response;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.OpenSearchClientFactory;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.ThreadProvider;

import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;

@ExtendWith(MockitoExtension.class)
public class SemanticEmbeddingBuilderTest {

	@Mock
	private LoggerFactory mockLoggerFactory;
	@Mock
	private Logger mockLogger;
	@Mock
	private OpenSearchClientFactory mockClientFactory;
	@Mock
	private OpenSearchGenericClient mockClient;
	@Mock
	private ThreadProvider mockThreadProvider;

	/**
	 * Responses queued by request path, and the paths actually requested in order. A path with more
	 * than one queued response (e.g. a models search hit before and after a deploy) is drained in
	 * order; a path with exactly one keeps returning it, matching a real poll loop.
	 */
	private Map<String, Deque<Stub>> responseByPath;
	private List<String> requestedPaths;
	private List<String> requestBodies;

	private static final String HOST = "search-dev-test-synidx.us-east-1.es.amazonaws.com";
	private static final String ROLE_ARN = "arn:aws:iam::123456789012:role/dev-test-synidx-bedrock-embed";

	private final Stack sharedResources = Stack.builder().outputs(
			Output.builder().outputKey(SemanticEmbeddingBuilder.OUTPUT_DOMAIN_ENDPOINT)
					.outputValue(HOST).build(),
			Output.builder().outputKey(SemanticEmbeddingBuilder.OUTPUT_BEDROCK_EMBED_ROLE_ARN).outputValue(ROLE_ARN)
					.build())
			.build();

	private SemanticEmbeddingBuilder builder;

	@BeforeEach
	public void before() {
		responseByPath = new HashMap<>();
		requestedPaths = new ArrayList<>();
		requestBodies = new ArrayList<>();
		when(mockLoggerFactory.getLogger(SemanticEmbeddingBuilder.class)).thenReturn(mockLogger);
		builder = new SemanticEmbeddingBuilder(mockLoggerFactory, mockClientFactory, mockThreadProvider,
				new TemplateGuiceModule().velocityEngineProvider());
	}

	@Test
	public void testBuildSemanticEmbeddingWithNothingProvisioned() throws Exception {
		setupHttp();
		// No system indexes yet, so every ML-Commons search 404s.
		respondNotFound("/_plugins/_ml/models/_search");
		respondNotFound("/_plugins/_ml/model_groups/_search");
		respondNotFound("/_plugins/_ml/connectors/_search");
		respond("/_plugins/_ml/model_groups/_register", "{\"model_group_id\":\"group-1\"}");
		respond("/_plugins/_ml/connectors/_create", "{\"connector_id\":\"connector-1\"}");
		respond("/_plugins/_ml/models/_register", "{\"model_id\":\"model-1\"}");
		respond("/_plugins/_ml/models/model-1/_deploy", "{\"task_id\":\"task-1\"}");
		respond("/_plugins/_ml/models/_search", hit("model-1", "\"model_state\":\"DEPLOYED\""));

		// call under test
		builder.buildSemanticEmbedding(sharedResources);

		assertEquals(List.of("/_plugins/_ml/models/_search", "/_plugins/_ml/model_groups/_search",
				"/_plugins/_ml/model_groups/_register", "/_plugins/_ml/connectors/_search",
				"/_plugins/_ml/connectors/_create", "/_plugins/_ml/models/_register",
				"/_plugins/_ml/models/model-1/_deploy", "/_plugins/_ml/models/_search"), requestedPaths);

		// The connector must name the role the domain template creates, and carry the model/dimension
		// the repository validates its indexes against.
		String connectorBody = requestBodies.get(4);
		assertTrue(connectorBody
				.contains("\"roleArn\":\"" + ROLE_ARN + "\""),
				connectorBody);
		assertTrue(connectorBody.contains("\"model\":\"amazon.titan-embed-text-v2:0\""), connectorBody);
		assertTrue(connectorBody.contains("\"dimensions\":1024"), connectorBody);
		// ML-Commons substitutes these at predict time, so Velocity must pass them through untouched.
		assertTrue(connectorBody.contains("\\\"inputText\\\": \\\"${parameters.inputText}\\\""), connectorBody);
		// Registering without an explicit group silently auto-creates a second one.
		assertTrue(requestBodies.get(5).contains("\"model_group_id\":\"group-1\""), requestBodies.get(5));
	}

	@Test
	public void testBuildSemanticEmbeddingWithModelAlreadyDeployed() throws Exception {
		setupHttp();
		respond("/_plugins/_ml/models/_search", hit("model-existing", "\"model_state\":\"DEPLOYED\""));

		// call under test
		builder.buildSemanticEmbedding(sharedResources);

		// Re-running a stack build must not mint a second connector or model.
		assertEquals(List.of("/_plugins/_ml/models/_search"), requestedPaths);
	}

	@Test
	public void testBuildSemanticEmbeddingWithExistingConnectorAndGroup() throws Exception {
		setupHttp();
		respondNotFound("/_plugins/_ml/models/_search");
		respond("/_plugins/_ml/model_groups/_search", hit("group-existing", "\"name\":\"synapse-semantic-embedding\""));
		respond("/_plugins/_ml/connectors/_search",
				hit("connector-existing", "\"name\":\"synapse-semantic-embedding-bedrock\""));
		respond("/_plugins/_ml/models/_register", "{\"model_id\":\"model-2\"}");
		respond("/_plugins/_ml/models/model-2/_deploy", "{}");
		respond("/_plugins/_ml/models/_search", hit("model-2", "\"model_state\":\"DEPLOYED\""));

		// call under test
		builder.buildSemanticEmbedding(sharedResources);

		assertTrue(requestBodies.get(3).contains("\"model_group_id\":\"group-existing\""), requestBodies.get(3));
		assertTrue(requestBodies.get(3).contains("\"connector_id\":\"connector-existing\""), requestBodies.get(3));
	}

	@Test
	public void testBuildSemanticEmbeddingWithDeployInProgress() throws Exception {
		setupHttp();
		respondNotFound("/_plugins/_ml/models/_search");
		respond("/_plugins/_ml/model_groups/_search", hit("group-existing", "\"name\":\"synapse-semantic-embedding\""));
		respond("/_plugins/_ml/connectors/_search",
				hit("connector-existing", "\"name\":\"synapse-semantic-embedding-bedrock\""));
		respond("/_plugins/_ml/models/_register", "{\"model_id\":\"model-3\"}");
		respond("/_plugins/_ml/models/model-3/_deploy", "{}");
		respond("/_plugins/_ml/models/_search", hit("model-3", "\"model_state\":\"DEPLOYING\""));
		respond("/_plugins/_ml/models/_search", hit("model-3", "\"model_state\":\"DEPLOYED\""));

		// call under test
		builder.buildSemanticEmbedding(sharedResources);

		verify(mockThreadProvider).sleep(SemanticEmbeddingBuilder.DEPLOY_POLL_INTERVAL_MS);
	}

	@Test
	public void testBuildSemanticEmbeddingWithFailedConnectorCreate() throws Exception {
		setupHttp();
		respondNotFound("/_plugins/_ml/models/_search");
		respondNotFound("/_plugins/_ml/model_groups/_search");
		respond("/_plugins/_ml/model_groups/_register", "{\"model_group_id\":\"group-1\"}");
		respondNotFound("/_plugins/_ml/connectors/_search");
		// The PassRole denial that motivated moving this step out of the repository.
		respondWithStatus("/_plugins/_ml/connectors/_create", 403,
				"{\"error\":{\"type\":\"security_exception\",\"reason\":\"not authorized to perform: iam:PassRole\"}}");

		String message = assertThrows(IllegalStateException.class, () -> builder.buildSemanticEmbedding(sharedResources))
				.getMessage();

		assertTrue(message.contains("iam:PassRole"), message);
		// Nothing is registered against a connector that was never created.
		assertTrue(!requestedPaths.contains("/_plugins/_ml/models/_register"), requestedPaths.toString());
	}

	@Test
	public void testBuildSemanticEmbeddingWithMissingOutput() {
		Stack withoutEndpoint = Stack.builder().outputs(Output.builder()
				.outputKey(SemanticEmbeddingBuilder.OUTPUT_BEDROCK_EMBED_ROLE_ARN).outputValue(ROLE_ARN).build())
				.build();

		String message = assertThrows(IllegalStateException.class,
				() -> builder.buildSemanticEmbedding(withoutEndpoint)).getMessage();

		assertEquals("Failed to find shared resources output: SynapseSearchIndexDomainEndpoint", message);
		verify(mockClientFactory, never()).getDomainGenericClient(any());
	}

	/**
	 * Records each request and replays the body registered for its path, so the assertions can read
	 * the JSON that would actually reach ML-Commons.
	 */
	private void setupHttp() throws Exception {
		when(mockClientFactory.getDomainGenericClient(HOST)).thenReturn(mockClient);
		when(mockClient.execute(any(Request.class))).thenAnswer(invocation -> {
			Request request = invocation.getArgument(0);
			String path = request.getEndpoint();
			requestedPaths.add(path);
			requestBodies.add(request.getBody().map(Body::bodyAsString).orElse(""));

			Deque<Stub> stubs = responseByPath.get(path);
			if (stubs == null || stubs.isEmpty()) {
				throw new IllegalStateException("No stubbed response for " + path);
			}
			Stub stub = stubs.size() > 1 ? stubs.poll() : stubs.peek();
			Response response = mock(Response.class);
			when(response.getStatus()).thenReturn(stub.status);
			when(response.getBody()).thenReturn(
					Optional.of(Body.from(stub.body.getBytes(StandardCharsets.UTF_8), "application/json")));
			return response;
		});
	}

	private static final class Stub {
		private final String body;
		private final int status;

		private Stub(String body, int status) {
			this.body = body;
			this.status = status;
		}
	}

	private void respond(String path, String body) {
		responseByPath.computeIfAbsent(path, p -> new ArrayDeque<>()).add(new Stub(body, 200));
	}

	private void respondNotFound(String path) {
		responseByPath.computeIfAbsent(path, p -> new ArrayDeque<>())
				.add(new Stub("{\"error\":\"index_not_found_exception\"}", 404));
	}

	private void respondWithStatus(String path, int status, String body) {
		responseByPath.computeIfAbsent(path, p -> new ArrayDeque<>()).add(new Stub(body, status));
	}

	private static String hit(String id, String source) {
		return "{\"hits\":{\"hits\":[{\"_id\":\"" + id + "\",\"_source\":{" + source + "}}]}}";
	}
}
