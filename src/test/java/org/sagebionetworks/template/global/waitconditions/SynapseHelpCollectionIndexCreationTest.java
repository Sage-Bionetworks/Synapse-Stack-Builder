package org.sagebionetworks.template.global.waitconditions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.OpenSearchClientFactory;
import org.sagebionetworks.template.WaitConditionHandler;
import org.sagebionetworks.template.config.RepoConfiguration;

import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionRequest;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionDetail;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionStatus;

@ExtendWith(MockitoExtension.class)
public class SynapseHelpCollectionIndexCreationTest {

	private final static String COLLECTION_ENDPOINT = "endpoint";
	
	@Mock
	private LoggerFactory mockLoggerFactory;
	
	@Mock
	private OpenSearchServerlessClient mockOssManagementClient;
	
	@Mock
	private OpenSearchClientFactory mockOpenSearchClientFactory;
	
	@Mock
	private RepoConfiguration mockConfig;
	
	private WaitConditionHandler handler;
	
	@Mock
	private Logger mockLogger;

	@Mock
	private OpenSearchIndicesClient mockOpenSearchIndicesClient;
	
	@Captor
	private ArgumentCaptor<Consumer<BatchGetCollectionRequest.Builder>> getCollectionRequestCaptor;
	
	@Captor
	private ArgumentCaptor<ExistsRequest> existRequestCaptor;
	
	@Captor
	private ArgumentCaptor<CreateIndexRequest> createRequestCaptor;
	
	@BeforeEach
	public void before() {
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		handler = new SynapseHelpCollectionIndexCreation(mockLoggerFactory, mockConfig, mockOssManagementClient, mockOpenSearchClientFactory);
	}
	
	@Test
	public void testGetWaitConditionId() {
		// Call under test
		assertEquals("SynapseHelpCollectionCreateIndexWaitCondition", handler.getWaitConditionId());
	}
	
	@Test
	public void testHandleWithActiveCollection() throws IOException, InterruptedException {
		
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");		
		
		when(mockOssManagementClient.batchGetCollection(getCollectionRequestCaptor.capture())).thenReturn(BatchGetCollectionResponse.builder()
			.collectionDetails(CollectionDetail.builder().status(CollectionStatus.ACTIVE).collectionEndpoint(COLLECTION_ENDPOINT).build()).build()
		);
		
		when(mockOpenSearchClientFactory.getIndicesClient(COLLECTION_ENDPOINT)).thenReturn(mockOpenSearchIndicesClient);
		
		when(mockOpenSearchIndicesClient.exists(existRequestCaptor.capture())).thenReturn(new BooleanResponse(false));
		when(mockOpenSearchIndicesClient.create(createRequestCaptor.capture())).thenReturn(
			CreateIndexResponse.of(resp -> resp
				.index("vector-idx")
				.acknowledged(true)
				.shardsAcknowledged(true)
			)
		);
		StackEvent stackEvent = StackEvent.builder().build();
		
		// Call under test
		assertEquals(Optional.of("index-creation-complete"), handler.handle(stackEvent));
		
		assertEquals(
			BatchGetCollectionRequest.builder().names("dev-synhelp").build(), 
			BatchGetCollectionRequest.builder().applyMutation(getCollectionRequestCaptor.getValue()).build()
		);
		
		ExistsRequest existRequest = existRequestCaptor.getValue();
		
		assertEquals(List.of("vector-idx"), existRequest.index());
		
		CreateIndexRequest createRequest = createRequestCaptor.getValue();
		
		assertEquals("vector-idx", createRequest.index());
		assertTrue(createRequest.settings().knn());
		assertEquals(512, createRequest.settings().knnAlgoParamEfSearch());
		
		Property textVectorProp = createRequest.mappings().properties().get("text_vector");
		
		assertEquals(1024, textVectorProp.knnVector().dimension());
		assertEquals("hnsw", textVectorProp.knnVector().method().name());
		assertEquals("faiss", textVectorProp.knnVector().method().engine());
		assertEquals("l2", textVectorProp.knnVector().method().spaceType());
		assertTrue(createRequest.mappings().properties().get("text_raw").text().index());
		assertFalse(createRequest.mappings().properties().get("text_metadata").text().index());
		
	}
	
	@Test
	public void testHandleWithInactiveCollection() throws IOException, InterruptedException {
		
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");		
		
		when(mockOssManagementClient.batchGetCollection(getCollectionRequestCaptor.capture())).thenReturn(BatchGetCollectionResponse.builder()
			.collectionDetails(CollectionDetail.builder().status(CollectionStatus.CREATING).collectionEndpoint(COLLECTION_ENDPOINT).build()).build()
		);
		StackEvent stackEvent = StackEvent.builder().build();

		// Call under test
		assertEquals(Optional.empty(), handler.handle(stackEvent));
		
		verifyNoMoreInteractions(mockOpenSearchIndicesClient);
		
	}
	
	@Test
	public void testHandleWithCollectionNotFound() throws IOException, InterruptedException {
		
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");		
		
		when(mockOssManagementClient.batchGetCollection(getCollectionRequestCaptor.capture())).thenReturn(BatchGetCollectionResponse.builder()
			.collectionDetails(Collections.emptyList()).build()
		);
		StackEvent stackEvent = StackEvent.builder().build();

		assertThrows(NoSuchElementException.class, () -> {			
			// Call under test
			handler.handle(stackEvent);
		});
				
		verifyNoMoreInteractions(mockOpenSearchIndicesClient);
	
	}
	
	@Test
	public void testHandleWithExistingIndex() throws IOException, InterruptedException {
		
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");		
		
		when(mockOssManagementClient.batchGetCollection(getCollectionRequestCaptor.capture())).thenReturn(BatchGetCollectionResponse.builder()
			.collectionDetails(CollectionDetail.builder().status(CollectionStatus.ACTIVE).collectionEndpoint(COLLECTION_ENDPOINT).build()).build()
		);
		
		when(mockOpenSearchClientFactory.getIndicesClient(COLLECTION_ENDPOINT)).thenReturn(mockOpenSearchIndicesClient);
		
		when(mockOpenSearchIndicesClient.exists(existRequestCaptor.capture())).thenReturn(new BooleanResponse(true));
		StackEvent stackEvent = StackEvent.builder().build();

		// Call under test
		assertEquals(Optional.of("index-already-exists"), handler.handle(stackEvent));
		
		assertEquals(
			BatchGetCollectionRequest.builder().names("dev-synhelp").build(), 
			BatchGetCollectionRequest.builder().applyMutation(getCollectionRequestCaptor.getValue()).build()
		);
		
		ExistsRequest existRequest = existRequestCaptor.getValue();
		
		assertEquals(List.of("vector-idx"), existRequest.index());
		
		verifyNoMoreInteractions(mockOpenSearchIndicesClient);
	
	}
	
	@Test
	public void testHandleWithIOException() throws IOException {
		
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");		
		
		when(mockOssManagementClient.batchGetCollection(getCollectionRequestCaptor.capture())).thenReturn(BatchGetCollectionResponse.builder()
			.collectionDetails(CollectionDetail.builder().status(CollectionStatus.ACTIVE).collectionEndpoint(COLLECTION_ENDPOINT).build()).build()
		);
		
		when(mockOpenSearchClientFactory.getIndicesClient(COLLECTION_ENDPOINT)).thenReturn(mockOpenSearchIndicesClient);
		
		IOException ex = new IOException("nope");
		
		when(mockOpenSearchIndicesClient.exists(existRequestCaptor.capture())).thenThrow(ex);

		StackEvent stackEvent = StackEvent.builder().build();

		IllegalStateException result = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			handler.handle(stackEvent);
		});
		
		assertEquals(ex, result.getCause());
		
		assertEquals(
			BatchGetCollectionRequest.builder().names("dev-synhelp").build(), 
			BatchGetCollectionRequest.builder().applyMutation(getCollectionRequestCaptor.getValue()).build()
		);
		
		ExistsRequest existRequest = existRequestCaptor.getValue();
		
		assertEquals(List.of("vector-idx"), existRequest.index());
		
		verifyNoMoreInteractions(mockOpenSearchIndicesClient);
	
	}
	
	@Test
	public void testHandleWithRetryOnOpenSearchException() throws IOException, InterruptedException {
		StackEvent stackEvent = StackEvent.builder().build();

		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");		
		
		when(mockOssManagementClient.batchGetCollection(getCollectionRequestCaptor.capture())).thenReturn(BatchGetCollectionResponse.builder()
			.collectionDetails(CollectionDetail.builder().status(CollectionStatus.ACTIVE).collectionEndpoint(COLLECTION_ENDPOINT).build()).build()
		);
		
		when(mockOpenSearchClientFactory.getIndicesClient(COLLECTION_ENDPOINT)).thenReturn(mockOpenSearchIndicesClient);
		
		OpenSearchException ex = new OpenSearchException(new ErrorResponse.Builder().error(new ErrorCause.Builder().type("nope").reason("bad").build()).build());
		
		when(mockOpenSearchIndicesClient.exists(existRequestCaptor.capture())).thenThrow(ex);
		
		for (int i = 0; i < SynapseHelpCollectionIndexCreation.MAX_RETRY_COUNT; i++) {
			// Call under test
			assertEquals(Optional.empty(), handler.handle(stackEvent));
		}
		
		assertEquals(ex, assertThrows(OpenSearchException.class, () -> {
			handler.handle(stackEvent).isEmpty();
		}));
		
		verifyNoMoreInteractions(mockOpenSearchIndicesClient);
	}
	
	@Test
	public void testHandleWithRetryOnOpenSearchExceptionAndSuccess() throws IOException, InterruptedException {
		StackEvent stackEvent = StackEvent.builder().build();
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");		
		
		when(mockOssManagementClient.batchGetCollection(getCollectionRequestCaptor.capture())).thenReturn(BatchGetCollectionResponse.builder()
			.collectionDetails(CollectionDetail.builder().status(CollectionStatus.ACTIVE).collectionEndpoint(COLLECTION_ENDPOINT).build()).build()
		);
		
		when(mockOpenSearchClientFactory.getIndicesClient(COLLECTION_ENDPOINT)).thenReturn(mockOpenSearchIndicesClient);
		
		OpenSearchException ex = new OpenSearchException(new ErrorResponse.Builder().error(new ErrorCause.Builder().type("nope").reason("bad").build()).build());
		
		when(mockOpenSearchIndicesClient.exists(existRequestCaptor.capture())).thenThrow(ex).thenReturn(new BooleanResponse(true));
		
		assertEquals(Optional.empty(), handler.handle(stackEvent));
		assertEquals(Optional.of("index-already-exists"), handler.handle(stackEvent));
				
		verifyNoMoreInteractions(mockOpenSearchIndicesClient);
	}	

}
