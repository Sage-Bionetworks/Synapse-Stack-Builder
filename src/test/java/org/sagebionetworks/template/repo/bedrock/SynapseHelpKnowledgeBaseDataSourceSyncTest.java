package org.sagebionetworks.template.repo.bedrock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.ThreadProvider;
import org.sagebionetworks.template.WaitConditionHandler;
import org.sagebionetworks.template.config.RepoConfiguration;

import software.amazon.awssdk.services.bedrockagent.BedrockAgentClient;
import software.amazon.awssdk.services.bedrockagent.model.DataSourceSummary;
import software.amazon.awssdk.services.bedrockagent.model.GetIngestionJobRequest;
import software.amazon.awssdk.services.bedrockagent.model.GetIngestionJobResponse;
import software.amazon.awssdk.services.bedrockagent.model.IngestionJob;
import software.amazon.awssdk.services.bedrockagent.model.IngestionJobStatus;
import software.amazon.awssdk.services.bedrockagent.model.IngestionJobSummary;
import software.amazon.awssdk.services.bedrockagent.model.KnowledgeBaseSummary;
import software.amazon.awssdk.services.bedrockagent.model.ListDataSourcesRequest;
import software.amazon.awssdk.services.bedrockagent.model.ListDataSourcesResponse;
import software.amazon.awssdk.services.bedrockagent.model.ListIngestionJobsRequest;
import software.amazon.awssdk.services.bedrockagent.model.ListIngestionJobsResponse;
import software.amazon.awssdk.services.bedrockagent.model.ListKnowledgeBasesRequest;
import software.amazon.awssdk.services.bedrockagent.model.ListKnowledgeBasesResponse;
import software.amazon.awssdk.services.bedrockagent.model.StartIngestionJobRequest;
import software.amazon.awssdk.services.bedrockagent.model.StartIngestionJobResponse;
import software.amazon.awssdk.services.bedrockagent.paginators.ListDataSourcesIterable;
import software.amazon.awssdk.services.bedrockagent.paginators.ListKnowledgeBasesIterable;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;

@ExtendWith(MockitoExtension.class)
public class SynapseHelpKnowledgeBaseDataSourceSyncTest {

	private static final String KNOWLEDGE_BASE_ID = "123";
	private static final String DATA_SOURCE_ID = "456";
	private static final String JOB_HASH = "027161e3d3c63c5680c1e4d38da31892f5a32797600fb68bd1e9ab4bc95ceb8a";
	private static final String JOB_ID = "job-id";
	
	@Mock
	private LoggerFactory mockLoggerFactory;
	@Mock
	private BedrockAgentClient mockBedrockAgentClient;
	@Mock
	private RepoConfiguration mockConfig;
	@Mock
	private ThreadProvider mockThreadProvider;
	
	private WaitConditionHandler handler;
	
	@Mock
	private Logger mockLogger;
	
	@Mock
	private StackEvent mockStackEvent;
		
	@Captor
	private ArgumentCaptor<Consumer<ListKnowledgeBasesRequest.Builder>> listKnowledgeBasesRequestCaptor;
	@Captor
	private ArgumentCaptor<Consumer<ListDataSourcesRequest.Builder>> listDataSourceRequestCaptor;
	@Captor
	private ArgumentCaptor<Consumer<ListIngestionJobsRequest.Builder>> listIngestionRequestCaptor;
	@Captor
	private ArgumentCaptor<Consumer<StartIngestionJobRequest.Builder>> startIngestionJobRequestCaptor;
	@Captor
	private ArgumentCaptor<Consumer<GetIngestionJobRequest.Builder>> getIngestionJobRequestCaptor;
	
	@BeforeEach
	public void before() {
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		handler = new SynapseHelpKnowledgeBaseDataSourceSync(mockLoggerFactory, mockConfig, mockThreadProvider, mockBedrockAgentClient);
	}
	
	@Test
	public void testGetWaitConditionId() {
		assertEquals("SynapseHelpKnowledgeBaseDataSourceSyncWaitCondition", handler.getWaitConditionId());
	}

	@Test
	public void testHandle() throws InterruptedException {
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_INSTANCE)).thenReturn("101");
		
		String knowledgeBaseName = "dev-101-synhelp-knowledge-base";
		
		when(mockBedrockAgentClient.listKnowledgeBases(any(ListKnowledgeBasesRequest.class))).thenReturn(
			ListKnowledgeBasesResponse.builder().knowledgeBaseSummaries(List.of(
				KnowledgeBaseSummary.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).name(knowledgeBaseName).build()
			)).build()
		);
		
		when(mockBedrockAgentClient.listKnowledgeBasesPaginator(listKnowledgeBasesRequestCaptor.capture())).thenReturn(
			new ListKnowledgeBasesIterable(mockBedrockAgentClient, ListKnowledgeBasesRequest.builder().build())
		);
		
		String dataSourceName = "dev-101-synhelp-datasource";
		
		when(mockBedrockAgentClient.listDataSources(any(ListDataSourcesRequest.class))).thenReturn(
			ListDataSourcesResponse.builder().dataSourceSummaries(List.of(
				DataSourceSummary.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).dataSourceId(DATA_SOURCE_ID).name(dataSourceName).build()
			)).build()
		);
		
		when(mockBedrockAgentClient.listDataSourcesPaginator(listDataSourceRequestCaptor.capture())).thenReturn(
			new ListDataSourcesIterable(mockBedrockAgentClient, ListDataSourcesRequest.builder().build())
		);
		
		when(mockBedrockAgentClient.listIngestionJobs(listIngestionRequestCaptor.capture())).thenReturn(ListIngestionJobsResponse.builder().build());
		
		IngestionJob job = IngestionJob.builder()
			.knowledgeBaseId(KNOWLEDGE_BASE_ID)
			.dataSourceId(DATA_SOURCE_ID)
			.ingestionJobId(JOB_ID)
			.status(IngestionJobStatus.STARTING)
			.build();
		
		when(mockBedrockAgentClient.startIngestionJob(startIngestionJobRequestCaptor.capture())).thenReturn(
			StartIngestionJobResponse.builder().ingestionJob(job).build()
		);
		
		when(mockBedrockAgentClient.getIngestionJob(getIngestionJobRequestCaptor.capture())).thenReturn(
			GetIngestionJobResponse.builder().ingestionJob(job.copy(b -> b
				.status(IngestionJobStatus.COMPLETE)
				.statistics(stats -> stats
					.numberOfDocumentsScanned(10L)
					.numberOfNewDocumentsIndexed(5L)
					.numberOfDocumentsFailed(5L)
				)
			)).build()
		);
		
		// Call under test
		assertEquals(Optional.of("sync-completed"), handler.handle(mockStackEvent));
		
		assertEquals(
			ListKnowledgeBasesRequest.builder().build(),
			ListKnowledgeBasesRequest.builder().applyMutation(listKnowledgeBasesRequestCaptor.getValue()).build()
		);
		
		assertEquals(
			ListDataSourcesRequest.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).build(),
			ListDataSourcesRequest.builder().applyMutation(listDataSourceRequestCaptor.getValue()).build()
		);
		
		assertEquals(
			ListIngestionJobsRequest.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).dataSourceId(DATA_SOURCE_ID).maxResults(1).build(),
			ListIngestionJobsRequest.builder().applyMutation(listIngestionRequestCaptor.getValue()).build()
		);
		
		assertEquals(
			StartIngestionJobRequest.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).dataSourceId(DATA_SOURCE_ID).clientToken(JOB_HASH).build(),
			StartIngestionJobRequest.builder().applyMutation(startIngestionJobRequestCaptor.getValue()).build()
		);
		
		assertEquals(
			GetIngestionJobRequest.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).dataSourceId(DATA_SOURCE_ID).ingestionJobId(JOB_ID).build(),
			GetIngestionJobRequest.builder().applyMutation(getIngestionJobRequestCaptor.getValue()).build()
		);
	}
	
	@Test
	public void testHandleWithKnowledgeBaseNotFound() throws InterruptedException {
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_INSTANCE)).thenReturn("101");
		
		when(mockBedrockAgentClient.listKnowledgeBases(any(ListKnowledgeBasesRequest.class))).thenReturn(
			ListKnowledgeBasesResponse.builder().knowledgeBaseSummaries(Collections.emptyList()).build()
		);
		
		when(mockBedrockAgentClient.listKnowledgeBasesPaginator(listKnowledgeBasesRequestCaptor.capture())).thenReturn(
			new ListKnowledgeBasesIterable(mockBedrockAgentClient, ListKnowledgeBasesRequest.builder().build())
		);		
		
		assertThrows(NoSuchElementException.class, () -> {
			// Call under test
			handler.handle(mockStackEvent);
		});
		
		assertEquals(
			ListKnowledgeBasesRequest.builder().build(),
			ListKnowledgeBasesRequest.builder().applyMutation(listKnowledgeBasesRequestCaptor.getValue()).build()
		);
		
		verifyNoMoreInteractions(mockBedrockAgentClient);
	}
	
	@Test
	public void testHandleWithDataSourceNotFound() throws InterruptedException {
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_INSTANCE)).thenReturn("101");
		
		String knowledgeBaseName = "dev-101-synhelp-knowledge-base";
		
		when(mockBedrockAgentClient.listKnowledgeBases(any(ListKnowledgeBasesRequest.class))).thenReturn(
			ListKnowledgeBasesResponse.builder().knowledgeBaseSummaries(List.of(
				KnowledgeBaseSummary.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).name(knowledgeBaseName).build()
			)).build()
		);
		
		when(mockBedrockAgentClient.listKnowledgeBasesPaginator(listKnowledgeBasesRequestCaptor.capture())).thenReturn(
			new ListKnowledgeBasesIterable(mockBedrockAgentClient, ListKnowledgeBasesRequest.builder().build())
		);
				
		when(mockBedrockAgentClient.listDataSources(any(ListDataSourcesRequest.class))).thenReturn(
			ListDataSourcesResponse.builder().dataSourceSummaries(Collections.emptyList()).build()
		);
		
		when(mockBedrockAgentClient.listDataSourcesPaginator(listDataSourceRequestCaptor.capture())).thenReturn(
			new ListDataSourcesIterable(mockBedrockAgentClient, ListDataSourcesRequest.builder().build())
		);
				
		assertThrows(NoSuchElementException.class, () -> {
			// Call under test
			handler.handle(mockStackEvent);
		});
		
		assertEquals(
			ListKnowledgeBasesRequest.builder().build(),
			ListKnowledgeBasesRequest.builder().applyMutation(listKnowledgeBasesRequestCaptor.getValue()).build()
		);
		
		assertEquals(
			ListDataSourcesRequest.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).build(),
			ListDataSourcesRequest.builder().applyMutation(listDataSourceRequestCaptor.getValue()).build()
		);
		
		verifyNoMoreInteractions(mockBedrockAgentClient);
	}
	
	@Test
	public void testHandleWithJobAlreadyStarted() throws InterruptedException {
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_INSTANCE)).thenReturn("101");
		
		String knowledgeBaseName = "dev-101-synhelp-knowledge-base";
		
		when(mockBedrockAgentClient.listKnowledgeBases(any(ListKnowledgeBasesRequest.class))).thenReturn(
			ListKnowledgeBasesResponse.builder().knowledgeBaseSummaries(List.of(
				KnowledgeBaseSummary.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).name(knowledgeBaseName).build()
			)).build()
		);
		
		when(mockBedrockAgentClient.listKnowledgeBasesPaginator(listKnowledgeBasesRequestCaptor.capture())).thenReturn(
			new ListKnowledgeBasesIterable(mockBedrockAgentClient, ListKnowledgeBasesRequest.builder().build())
		);
		
		String dataSourceName = "dev-101-synhelp-datasource";
		
		when(mockBedrockAgentClient.listDataSources(any(ListDataSourcesRequest.class))).thenReturn(
			ListDataSourcesResponse.builder().dataSourceSummaries(List.of(
				DataSourceSummary.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).dataSourceId(DATA_SOURCE_ID).name(dataSourceName).build()
			)).build()
		);
		
		when(mockBedrockAgentClient.listDataSourcesPaginator(listDataSourceRequestCaptor.capture())).thenReturn(
			new ListDataSourcesIterable(mockBedrockAgentClient, ListDataSourcesRequest.builder().build())
		);
		
		when(mockBedrockAgentClient.listIngestionJobs(listIngestionRequestCaptor.capture())).thenReturn(
			ListIngestionJobsResponse.builder().ingestionJobSummaries(List.of(
				IngestionJobSummary.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).dataSourceId(DATA_SOURCE_ID).ingestionJobId(JOB_ID).build()
			)).build()
		);
				
		// Call under test
		assertEquals(Optional.of("sync-started"), handler.handle(mockStackEvent));
		
		assertEquals(
			ListKnowledgeBasesRequest.builder().build(),
			ListKnowledgeBasesRequest.builder().applyMutation(listKnowledgeBasesRequestCaptor.getValue()).build()
		);
		
		assertEquals(
			ListDataSourcesRequest.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).build(),
			ListDataSourcesRequest.builder().applyMutation(listDataSourceRequestCaptor.getValue()).build()
		);
		
		assertEquals(
			ListIngestionJobsRequest.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).dataSourceId(DATA_SOURCE_ID).maxResults(1).build(),
			ListIngestionJobsRequest.builder().applyMutation(listIngestionRequestCaptor.getValue()).build()
		);
		
		verifyNoMoreInteractions(mockBedrockAgentClient);
	}
	
	@Test
	public void testHandleWithJobFailure() throws InterruptedException {
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_STACK)).thenReturn("dev");
		when(mockConfig.getProperty(Constants.PROPERTY_KEY_INSTANCE)).thenReturn("101");
		
		String knowledgeBaseName = "dev-101-synhelp-knowledge-base";
		
		when(mockBedrockAgentClient.listKnowledgeBases(any(ListKnowledgeBasesRequest.class))).thenReturn(
			ListKnowledgeBasesResponse.builder().knowledgeBaseSummaries(List.of(
				KnowledgeBaseSummary.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).name(knowledgeBaseName).build()
			)).build()
		);
		
		when(mockBedrockAgentClient.listKnowledgeBasesPaginator(listKnowledgeBasesRequestCaptor.capture())).thenReturn(
			new ListKnowledgeBasesIterable(mockBedrockAgentClient, ListKnowledgeBasesRequest.builder().build())
		);
		
		String dataSourceName = "dev-101-synhelp-datasource";
		
		when(mockBedrockAgentClient.listDataSources(any(ListDataSourcesRequest.class))).thenReturn(
			ListDataSourcesResponse.builder().dataSourceSummaries(List.of(
				DataSourceSummary.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).dataSourceId(DATA_SOURCE_ID).name(dataSourceName).build()
			)).build()
		);
		
		when(mockBedrockAgentClient.listDataSourcesPaginator(listDataSourceRequestCaptor.capture())).thenReturn(
			new ListDataSourcesIterable(mockBedrockAgentClient, ListDataSourcesRequest.builder().build())
		);
		
		when(mockBedrockAgentClient.listIngestionJobs(listIngestionRequestCaptor.capture())).thenReturn(ListIngestionJobsResponse.builder().build());
		
		IngestionJob job = IngestionJob.builder()
			.knowledgeBaseId(KNOWLEDGE_BASE_ID)
			.dataSourceId(DATA_SOURCE_ID)
			.ingestionJobId(JOB_ID)
			.status(IngestionJobStatus.STARTING)
			.build();
		
		when(mockBedrockAgentClient.startIngestionJob(startIngestionJobRequestCaptor.capture())).thenReturn(
			StartIngestionJobResponse.builder().ingestionJob(job).build()
		);
		
		when(mockBedrockAgentClient.getIngestionJob(getIngestionJobRequestCaptor.capture())).thenReturn(
			GetIngestionJobResponse.builder().ingestionJob(job).build(),
			GetIngestionJobResponse.builder().ingestionJob(job.copy(b -> b
				.status(IngestionJobStatus.FAILED)
				.failureReasons("Some failure")
			)).build()
		);
		
		assertEquals("Sync job job-id failed (Status: FAILED, Failures: [Some failure])", assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			handler.handle(mockStackEvent);
		}).getMessage());
		
		verify(mockBedrockAgentClient, times(2)).getIngestionJob(getIngestionJobRequestCaptor.capture());
		
		assertEquals(
			ListKnowledgeBasesRequest.builder().build(),
			ListKnowledgeBasesRequest.builder().applyMutation(listKnowledgeBasesRequestCaptor.getValue()).build()
		);
		
		assertEquals(
			ListDataSourcesRequest.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).build(),
			ListDataSourcesRequest.builder().applyMutation(listDataSourceRequestCaptor.getValue()).build()
		);
		
		assertEquals(
			ListIngestionJobsRequest.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).dataSourceId(DATA_SOURCE_ID).maxResults(1).build(),
			ListIngestionJobsRequest.builder().applyMutation(listIngestionRequestCaptor.getValue()).build()
		);
		
		assertEquals(
			StartIngestionJobRequest.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).dataSourceId(DATA_SOURCE_ID).clientToken(JOB_HASH).build(),
			StartIngestionJobRequest.builder().applyMutation(startIngestionJobRequestCaptor.getValue()).build()
		);
		
		assertEquals(
			GetIngestionJobRequest.builder().knowledgeBaseId(KNOWLEDGE_BASE_ID).dataSourceId(DATA_SOURCE_ID).ingestionJobId(JOB_ID).build(),
			GetIngestionJobRequest.builder().applyMutation(getIngestionJobRequestCaptor.getValue()).build()
		);
	}
}
