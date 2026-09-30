package org.sagebionetworks.template.repo.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.config.RepoConfiguration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.opensearch.OpenSearchClient;
import software.amazon.awssdk.services.opensearch.model.DescribeDomainRequest;
import software.amazon.awssdk.services.opensearch.model.DescribeDomainResponse;
import software.amazon.awssdk.services.opensearch.model.DomainStatus;

@ExtendWith(MockitoExtension.class)
public class SemanticEmbeddingBuilderTest {

	@Mock
	private LoggerFactory mockLoggerFactory;
	@Mock
	private Logger mockLogger;
	@Mock
	private RepoConfiguration mockConfig;
	@Mock
	private OpenSearchClient mockDomainManagementClient;
	@Mock
	private SdkHttpClient mockHttpClient;

	private final AwsCredentialsProvider credentialsProvider = () -> AwsBasicCredentials.create("id", "secret");

	/**
	 * Responses queued by request path, and the paths actually requested in order. A path with more
	 * than one queued response (e.g. a models search hit before and after a deploy) is drained in
	 * order; a path with exactly one keeps returning it, matching a real poll loop.
	 */
	private Map<String, Deque<Stub>> responseByPath;
	private List<String> requestedPaths;
	private List<String> requestBodies;

	private SemanticEmbeddingBuilder builder;

	@BeforeEach
	public void before() {
		responseByPath = new HashMap<>();
		requestedPaths = new ArrayList<>();
		requestBodies = new ArrayList<>();
		when(mockLoggerFactory.getLogger(SemanticEmbeddingBuilder.class)).thenReturn(mockLogger);
		builder = new SemanticEmbeddingBuilder(mockLoggerFactory, mockConfig, mockDomainManagementClient,
				mockHttpClient, credentialsProvider);
	}

	@Test
	public void testBuildSemanticEmbeddingWithNothingProvisioned() throws Exception {
		setupDomain("dev-test-synidx", "search-dev-test-synidx.us-east-1.es.amazonaws.com", null);
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
		builder.buildSemanticEmbedding();

		assertEquals(List.of("/_plugins/_ml/models/_search", "/_plugins/_ml/model_groups/_search",
				"/_plugins/_ml/model_groups/_register", "/_plugins/_ml/connectors/_search",
				"/_plugins/_ml/connectors/_create", "/_plugins/_ml/models/_register",
				"/_plugins/_ml/models/model-1/_deploy", "/_plugins/_ml/models/_search"), requestedPaths);

		// The connector must name the role the domain template creates, and carry the model/dimension
		// the repository validates its indexes against.
		String connectorBody = requestBodies.get(4);
		assertTrue(connectorBody
				.contains("\"roleArn\":\"arn:aws:iam::123456789012:role/dev-test-synidx-bedrock-embed\""),
				connectorBody);
		assertTrue(connectorBody.contains("\"model\":\"amazon.titan-embed-text-v2:0\""), connectorBody);
		assertTrue(connectorBody.contains("\"dimensions\":1024"), connectorBody);
		// Registering without an explicit group silently auto-creates a second one.
		assertTrue(requestBodies.get(5).contains("\"model_group_id\":\"group-1\""), requestBodies.get(5));
	}

	@Test
	public void testBuildSemanticEmbeddingWithModelAlreadyDeployed() throws Exception {
		setupDomain("dev-test-synidx", "search-dev-test-synidx.us-east-1.es.amazonaws.com", null);
		setupHttp();
		respond("/_plugins/_ml/models/_search", hit("model-existing", "\"model_state\":\"DEPLOYED\""));

		// call under test
		builder.buildSemanticEmbedding();

		// Re-running a stack build must not mint a second connector or model.
		assertEquals(List.of("/_plugins/_ml/models/_search"), requestedPaths);
	}

	@Test
	public void testBuildSemanticEmbeddingWithExistingConnectorAndGroup() throws Exception {
		setupDomain("dev-test-synidx", "search-dev-test-synidx.us-east-1.es.amazonaws.com", null);
		setupHttp();
		respondNotFound("/_plugins/_ml/models/_search");
		respond("/_plugins/_ml/model_groups/_search", hit("group-existing", "\"name\":\"synapse-semantic-embedding\""));
		respond("/_plugins/_ml/connectors/_search",
				hit("connector-existing", "\"name\":\"synapse-semantic-embedding-bedrock\""));
		respond("/_plugins/_ml/models/_register", "{\"model_id\":\"model-2\"}");
		respond("/_plugins/_ml/models/model-2/_deploy", "{}");
		respond("/_plugins/_ml/models/_search", hit("model-2", "\"model_state\":\"DEPLOYED\""));

		// call under test
		builder.buildSemanticEmbedding();

		assertTrue(requestBodies.get(3).contains("\"model_group_id\":\"group-existing\""), requestBodies.get(3));
		assertTrue(requestBodies.get(3).contains("\"connector_id\":\"connector-existing\""), requestBodies.get(3));
	}

	@Test
	public void testBuildSemanticEmbeddingWithVpcDomain() throws Exception {
		setupDomain("prod-101-synidx", "ignored-public-endpoint",
				"vpc-prod-101-synidx.us-east-1.es.amazonaws.com");
		setupHttp();
		respond("/_plugins/_ml/models/_search", hit("model-existing", "\"model_state\":\"DEPLOYED\""));

		// call under test
		builder.buildSemanticEmbedding();

		assertEquals("https://vpc-prod-101-synidx.us-east-1.es.amazonaws.com/_plugins/_ml/models/_search",
				requestedUris.get(0));
	}

	@Test
	public void testBuildSemanticEmbeddingWithFailedConnectorCreate() throws Exception {
		setupDomain("dev-test-synidx", "search-dev-test-synidx.us-east-1.es.amazonaws.com", null);
		setupHttp();
		respondNotFound("/_plugins/_ml/models/_search");
		respondNotFound("/_plugins/_ml/model_groups/_search");
		respond("/_plugins/_ml/model_groups/_register", "{\"model_group_id\":\"group-1\"}");
		respondNotFound("/_plugins/_ml/connectors/_search");
		// The PassRole denial that motivated moving this step out of the repository.
		respondWithStatus("/_plugins/_ml/connectors/_create", 403,
				"{\"error\":{\"type\":\"security_exception\",\"reason\":\"not authorized to perform: iam:PassRole\"}}");

		String message = assertThrows(IllegalStateException.class, () -> builder.buildSemanticEmbedding())
				.getMessage();

		assertTrue(message.contains("iam:PassRole"), message);
		// Nothing is registered against a connector that was never created.
		assertTrue(!requestedPaths.contains("/_plugins/_ml/models/_register"), requestedPaths.toString());
	}

	@Test
	public void testBuildSemanticEmbeddingWithNoDomainEndpoint() {
		setupDomain("dev-test-synidx", null, null);

		String message = assertThrows(IllegalStateException.class, () -> builder.buildSemanticEmbedding())
				.getMessage();

		assertEquals("No endpoint for OpenSearch domain dev-test-synidx", message);
		verify(mockHttpClient, never()).prepareRequest(any());
	}

	private void setupDomain(String domainName, String publicEndpoint, String vpcEndpoint) {
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn(domainName.split("-")[0]);
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_INSTANCE)).thenReturn(domainName.split("-")[1]);
		DomainStatus.Builder status = DomainStatus.builder()
				.domainName(domainName)
				.arn("arn:aws:es:us-east-1:123456789012:domain/" + domainName);
		if (vpcEndpoint == null) {
			status.endpoint(publicEndpoint);
		} else {
			status.vpcOptions(vpc -> vpc.vpcId("vpc-1")).endpoints(Map.of("vpc", vpcEndpoint));
		}
		DescribeDomainResponse response = DescribeDomainResponse.builder().domainStatus(status.build()).build();
		when(mockDomainManagementClient.describeDomain(any(Consumer.class))).thenAnswer(invocation -> {
			Consumer<DescribeDomainRequest.Builder> req = invocation.getArgument(0);
			DescribeDomainRequest.Builder captured = DescribeDomainRequest.builder();
			req.accept(captured);
			assertEquals(domainName, captured.build().domainName());
			return response;
		});
	}

	private final List<String> requestedUris = new ArrayList<>();

	/**
	 * Records each signed request and replays the body registered for its path, so the assertions can
	 * read the JSON that would actually reach ML-Commons.
	 */
	private void setupHttp() {
		when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class))).thenAnswer(invocation -> {
			HttpExecuteRequest request = invocation.getArgument(0);
			String path = request.httpRequest().encodedPath();
			requestedPaths.add(path);
			requestedUris.add(request.httpRequest().getUri().toString());
			requestBodies.add(readRequestBody(request));

			Deque<Stub> stubs = responseByPath.get(path);
			if (stubs == null || stubs.isEmpty()) {
				throw new IllegalStateException("No stubbed response for " + path);
			}
			Stub stub = stubs.size() > 1 ? stubs.poll() : stubs.peek();
			ExecutableHttpRequest executable = mock(ExecutableHttpRequest.class);
			when(executable.call()).thenReturn(HttpExecuteResponse.builder()
					.response(SdkHttpResponse.builder().statusCode(stub.status).build())
					.responseBody(AbortableInputStream.create(toStream(stub.body)))
					.build());
			return executable;
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

	private static InputStream toStream(String body) {
		return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
	}

	private static String readRequestBody(HttpExecuteRequest request) throws Exception {
		Optional<software.amazon.awssdk.http.ContentStreamProvider> provider = request.contentStreamProvider();
		if (!provider.isPresent()) {
			return "";
		}
		try (InputStream in = provider.get().newStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
