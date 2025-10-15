package org.sagebionetworks.template.global.waitconditions;

import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.OpenSearchClientFactory;
import org.sagebionetworks.template.WaitConditionHandler;
import org.sagebionetworks.template.config.Configuration;

import com.google.inject.Inject;

import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionDetail;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionStatus;

/**
 * A wait condition that pauses the stack creation until the synapse help open search collection data access policy that
 * allows the deployer to create an index is consistent. 
 */
public class SynapseHelpCollectionReadyWaitCondition implements WaitConditionHandler {
	static final int MAX_RETRY_COUNT = 5;
	
	private static final String IDX_NAME = "vector-idx";
	
	private Logger logger;
	
	private Configuration config;
	
	private OpenSearchServerlessClient ossManagementClient;
	
	private OpenSearchClientFactory openSearchClientFactory;
	
	private int retryCount = 0;	
	
	@Inject
	public SynapseHelpCollectionReadyWaitCondition(LoggerFactory loggerFactory, Configuration config, OpenSearchServerlessClient ossClient, OpenSearchClientFactory openSearchClientFactory) {
		this.logger = loggerFactory.getLogger(SynapseHelpCollectionReadyWaitCondition.class);
		this.config = config;
		this.ossManagementClient = ossClient;
		this.openSearchClientFactory = openSearchClientFactory;
	}

	@Override
	public String getWaitConditionId() {
		return "SynapseHelpCollectionReadyWaitCondition";
	}

	@Override
	public Optional<String> handle(StackEvent stackEvent) throws InterruptedException { 
		String collectionName = config.getProperty(Constants.PROPERTY_KEY_STACK) + "-synhelp";
		
		CollectionDetail collection = ossManagementClient.batchGetCollection(req -> req
			.names(collectionName)
		).collectionDetails().stream().findFirst().orElseThrow();
		
		if (!CollectionStatus.ACTIVE.equals(collection.status())) {
			logger.warn("Collection {} not ready, status: {}", collectionName, collection.status());
			return Optional.empty();
		}
		
		OpenSearchIndicesClient client = openSearchClientFactory.getIndicesClient(collection.collectionEndpoint());
		
		try {
			
			// This operation fails if the data access policy is not propagated yet
			if (client.exists(req -> req.index(IDX_NAME)).value()) {
				logger.warn("Index {} already exists.", IDX_NAME);
				retryCount = 0;
				return Optional.of("index-already-exists");
			}
			
			return Optional.of("collection-ready");
			
		} catch (OpenSearchException e) {
			logger.warn("The collection {} might not be ready yet:", collectionName, e);
			
			retryCount++;
			
			if (retryCount <= MAX_RETRY_COUNT) {
				return Optional.empty();
			}
			
			throw e;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
