package org.sagebionetworks.template.repo.bedrock;

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
import java.util.function.Function;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.opensearch.client.util.ObjectBuilder;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.OpenSearchClientFactory;
import org.sagebionetworks.template.WaitConditionHandler;
import org.sagebionetworks.template.config.RepoConfiguration;

import com.amazonaws.services.cloudformation.model.StackEvent;

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
	private StackEvent mockStackEvent;
	
	@Mock
	private OpenSearchIndicesClient mockOpenSearchIndicesClient;
	
	@Captor
	private ArgumentCaptor<Consumer<BatchGetCollectionRequest.Builder>> getCollectionRequestCaptor;
	
	@Captor
	private ArgumentCaptor<Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>> existsCaptor;
	
	@Captor
	private ArgumentCaptor<Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>>> createIdxCaptor;
	
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
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_INSTANCE)).thenReturn("101");
		
		when(mockOssManagementClient.batchGetCollection(getCollectionRequestCaptor.capture())).thenReturn(BatchGetCollectionResponse.builder()
			.collectionDetails(CollectionDetail.builder().status(CollectionStatus.ACTIVE).collectionEndpoint(COLLECTION_ENDPOINT).build()).build()
		);
		
		when(mockOpenSearchClientFactory.getIndicesClient(COLLECTION_ENDPOINT)).thenReturn(mockOpenSearchIndicesClient);
		
		existsCaptor = ArgumentCaptor.forClass(Function.class);
		when(mockOpenSearchIndicesClient.exists(existsCaptor.capture())).thenReturn(new BooleanResponse(false));

		CreateIndexResponse expectedResponse = CreateIndexResponse.of(
				resp -> resp
				.index("vector-idx")
				.acknowledged(true)
				.shardsAcknowledged(true)
			);
		createIdxCaptor = ArgumentCaptor.forClass(Function.class);
		when(mockOpenSearchIndicesClient.create(createIdxCaptor.capture())).thenReturn(expectedResponse);

		// Call under test
		assertEquals(Optional.of("index-creation-complete"), handler.handle(mockStackEvent));
		
		assertEquals(
			BatchGetCollectionRequest.builder().names("dev-101-synhelp").build(), 
			BatchGetCollectionRequest.builder().applyMutation(getCollectionRequestCaptor.getValue()).build()
		);
		
		Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>> capturedFunction = existsCaptor.getValue();
		ExistsRequest request = capturedFunction.apply(new ExistsRequest.Builder()).build();
		assertEquals(List.of("vector-idx"), request.index());


		Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>> creatIdxCapturedFunction = createIdxCaptor.getValue();
		CreateIndexRequest createIdxReq = creatIdxCapturedFunction.apply(new CreateIndexRequest.Builder()).build();
		assertEquals("vector-idx", createIdxReq.index());
		assertTrue(createIdxReq.settings().knn());
		assertEquals(512, createIdxReq.settings().knnAlgoParamEfSearch());
		Property textVectorProp = createIdxReq.mappings().properties().get("text_vector");
		
		assertEquals(1024, textVectorProp.knnVector().dimension());
		assertEquals("hnsw", textVectorProp.knnVector().method().name());
		assertEquals("faiss", textVectorProp.knnVector().method().engine());
		assertEquals("l2", textVectorProp.knnVector().method().spaceType());
		assertTrue(createIdxReq.mappings().properties().get("text_raw").text().index());
		assertFalse(createIdxReq.mappings().properties().get("text_metadata").text().index());
		
	}
	
	@Test
	public void testHandleWithInactiveCollection() throws IOException, InterruptedException {
		
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_INSTANCE)).thenReturn("101");
		
		when(mockOssManagementClient.batchGetCollection(getCollectionRequestCaptor.capture())).thenReturn(BatchGetCollectionResponse.builder()
			.collectionDetails(CollectionDetail.builder().status(CollectionStatus.CREATING).collectionEndpoint(COLLECTION_ENDPOINT).build()).build()
		);
				
		// Call under test
		assertEquals(Optional.empty(), handler.handle(mockStackEvent));
		
		verifyNoMoreInteractions(mockOpenSearchIndicesClient);
		
	}
	
	@Test
	public void testHandleWithCollectionNotFound() throws IOException, InterruptedException {
		
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_INSTANCE)).thenReturn("101");
		
		when(mockOssManagementClient.batchGetCollection(getCollectionRequestCaptor.capture())).thenReturn(BatchGetCollectionResponse.builder()
			.collectionDetails(Collections.emptyList()).build()
		);
		
		assertThrows(NoSuchElementException.class, () -> {			
			// Call under test
			handler.handle(mockStackEvent);
		});
				
		verifyNoMoreInteractions(mockOpenSearchIndicesClient);
	
	}
	
	@Test
	public void testHandleWithExistingIndex() throws IOException, InterruptedException {
		
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_INSTANCE)).thenReturn("101");
		
		when(mockOssManagementClient.batchGetCollection(getCollectionRequestCaptor.capture())).thenReturn(BatchGetCollectionResponse.builder()
			.collectionDetails(CollectionDetail.builder().status(CollectionStatus.ACTIVE).collectionEndpoint(COLLECTION_ENDPOINT).build()).build()
		);
		
		when(mockOpenSearchClientFactory.getIndicesClient(COLLECTION_ENDPOINT)).thenReturn(mockOpenSearchIndicesClient);

		existsCaptor = ArgumentCaptor.forClass(Function.class);
		when(mockOpenSearchIndicesClient.exists(existsCaptor.capture())).thenReturn(new BooleanResponse(true));

		// Call under test
		assertEquals(Optional.of("index-already-exists"), handler.handle(mockStackEvent));
		
		assertEquals(
			BatchGetCollectionRequest.builder().names("dev-101-synhelp").build(), 
			BatchGetCollectionRequest.builder().applyMutation(getCollectionRequestCaptor.getValue()).build()
		);

		Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>> capturedFunction = existsCaptor.getValue();
		ExistsRequest request = capturedFunction.apply(new ExistsRequest.Builder()).build();
		assertEquals(List.of("vector-idx"), request.index());
		
		verifyNoMoreInteractions(mockOpenSearchIndicesClient);
	
	}
	
	@Test
	public void testHandleWithIOException() throws IOException {
		
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_INSTANCE)).thenReturn("101");
		
		when(mockOssManagementClient.batchGetCollection(getCollectionRequestCaptor.capture())).thenReturn(BatchGetCollectionResponse.builder()
			.collectionDetails(CollectionDetail.builder().status(CollectionStatus.ACTIVE).collectionEndpoint(COLLECTION_ENDPOINT).build()).build()
		);
		
		when(mockOpenSearchClientFactory.getIndicesClient(COLLECTION_ENDPOINT)).thenReturn(mockOpenSearchIndicesClient);
		
		IOException ex = new IOException("nope");
		
		existsCaptor = ArgumentCaptor.forClass(Function.class);
		when(mockOpenSearchIndicesClient.exists(existsCaptor.capture())).thenThrow(ex);


		IllegalStateException result = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			handler.handle(mockStackEvent);
		});
		
		assertEquals(ex, result.getCause());
		
		assertEquals(
			BatchGetCollectionRequest.builder().names("dev-101-synhelp").build(), 
			BatchGetCollectionRequest.builder().applyMutation(getCollectionRequestCaptor.getValue()).build()
		);

		Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>> capturedFct  = existsCaptor.getValue();
		ExistsRequest request = capturedFct.apply(new ExistsRequest.Builder()).build();
		assertEquals(List.of("vector-idx"), request.index());

		verifyNoMoreInteractions(mockOpenSearchIndicesClient);
	
	}
	

}
