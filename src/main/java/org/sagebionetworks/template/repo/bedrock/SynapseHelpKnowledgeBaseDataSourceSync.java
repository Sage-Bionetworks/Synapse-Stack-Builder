package org.sagebionetworks.template.repo.bedrock;

import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.ThreadProvider;
import org.sagebionetworks.template.WaitConditionHandler;
import org.sagebionetworks.template.config.RepoConfiguration;

import com.google.inject.Inject;

import software.amazon.awssdk.services.bedrockagent.BedrockAgentClient;
import software.amazon.awssdk.services.bedrockagent.model.DataSourceSummary;
import software.amazon.awssdk.services.bedrockagent.model.IngestionJob;
import software.amazon.awssdk.services.bedrockagent.model.IngestionJobStatistics;
import software.amazon.awssdk.services.bedrockagent.model.IngestionJobSummary;
import software.amazon.awssdk.services.bedrockagent.model.KnowledgeBaseSummary;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;

/**
 * When a bedrock knowledge base is created its data source needs to be synchronized, we do this after the datasource is created through a 
 * wait condition using the bedrock APIs.
 */
public class SynapseHelpKnowledgeBaseDataSourceSync implements WaitConditionHandler {

	private static final long SLEEP_MS = 10_000;
	
	private Logger logger;
	private RepoConfiguration config;
	private ThreadProvider threadProvider;
	private BedrockAgentClient bedrockAgentClient;
	
	@Inject
	public SynapseHelpKnowledgeBaseDataSourceSync(LoggerFactory loggerFactory, RepoConfiguration config, ThreadProvider threadProvider, BedrockAgentClient bedrockAgentClient) {
		this.logger = loggerFactory.getLogger(SynapseHelpKnowledgeBaseDataSourceSync.class);
		this.config = config;
		this.threadProvider = threadProvider;
		this.bedrockAgentClient = bedrockAgentClient;
	}

	@Override
	public String getWaitConditionId() {
		return "SynapseHelpKnowledgeBaseDataSourceSyncWaitCondition";
	}

	@Override
	public Optional<String> handle(StackEvent stackEvent) throws InterruptedException {
		String stackPrefix = config.getProperty(Constants.PROPERTY_KEY_STACK) + "-" + config.getProperty(Constants.PROPERTY_KEY_INSTANCE);
		
		String knowledgeBaseName =  stackPrefix + "-synhelp-knowledge-base";
		String knowledgeBaseId = bedrockAgentClient.listKnowledgeBasesPaginator(req -> {})
			.knowledgeBaseSummaries().stream()
			.filter(kb -> kb.name().equals(knowledgeBaseName))
			.findFirst()
			.map(KnowledgeBaseSummary::knowledgeBaseId)
			.orElseThrow();
		
		String dataSourceName = stackPrefix + "-synhelp-datasource";
		String dataSourceId = bedrockAgentClient.listDataSourcesPaginator(req -> req.knowledgeBaseId(knowledgeBaseId))
			.dataSourceSummaries().stream()
			.filter(dataSource -> dataSource.name().equals(dataSourceName))
			.findFirst()
			.map(DataSourceSummary::dataSourceId)
			.orElseThrow();
		
		Optional<IngestionJobSummary> existingJob = bedrockAgentClient.listIngestionJobs(req -> req.knowledgeBaseId(knowledgeBaseId).dataSourceId(dataSourceId).maxResults(1))
			.ingestionJobSummaries()
			.stream()
			.findFirst();
		
		if (existingJob.isPresent()) {
			logger.warn("Sync job {} already exists (Status: {}).", existingJob.get().ingestionJobId(), existingJob.get().statusAsString());
			return Optional.of("sync-started");
		}
		
		String clientToken = DigestUtils.sha256Hex(knowledgeBaseId + " - " + dataSourceId);
			
		IngestionJob job = bedrockAgentClient.startIngestionJob(req -> req
			.clientToken(clientToken)
			.dataSourceId(dataSourceId)
			.knowledgeBaseId(knowledgeBaseId)
		).ingestionJob();
		
		String jobId = job.ingestionJobId();
		boolean done = false;
		
		do {
			logger.info("Waiting for sync job {} to complete (Status: {}).", job.ingestionJobId(), job.statusAsString());
			
			threadProvider.sleep(SLEEP_MS);
			
			job = bedrockAgentClient.getIngestionJob(req -> req
				.ingestionJobId(jobId)
				.knowledgeBaseId(knowledgeBaseId)
				.dataSourceId(dataSourceId)
			).ingestionJob();
			
			switch (job.status()) {
			case COMPLETE:
				IngestionJobStatistics stats = job.statistics();
				
				logger.info("Sync job {} completed (Documents Scanned: {}, Documents Indexed: {}, Documents Failed: {}).",
					job.ingestionJobId(),
					stats.numberOfDocumentsScanned(),
					stats.numberOfNewDocumentsIndexed(),
					stats.numberOfDocumentsFailed()
				);
				
				done = true;
				break;
			case FAILED:
			case STOPPED:
			case UNKNOWN_TO_SDK_VERSION:
				throw new IllegalStateException("Sync job " + jobId + " failed (Status: " + job.statusAsString() + ", Failures: " + job.failureReasons().toString() +")");
			default:
				break;
			}
			
		} while (!done);
		
		return Optional.of("sync-completed");
	}

}
