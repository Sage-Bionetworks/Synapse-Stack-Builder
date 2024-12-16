package org.sagebionetworks.template.repo.oss;

import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.WaitConditionHandler;
import org.sagebionetworks.template.config.RepoConfiguration;

import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.google.inject.Inject;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionDetail;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionStatus;

public class SynapseHelpCollectionIndexCreation implements WaitConditionHandler {
	
	private static final String IDX_NAME = "vector-idx";
	
	private Logger logger;
	
	private OpenSearchServerlessClient ossClient;
	
	private RepoConfiguration config;
	
	@Inject
	public SynapseHelpCollectionIndexCreation(LoggerFactory loggerFactory, OpenSearchServerlessClient ossClient, RepoConfiguration config) {
		this.logger = loggerFactory.getLogger(SynapseHelpCollectionIndexCreation.class);
		this.ossClient = ossClient;
		this.config = config;
	}
	
	@Override
	public String getWaitConditionId() {
		return "SynapseHelpCollectionCreateIndexWaitCondition";
	}
	
	@Override
	public String getSignalId() {
		return "vector-index-created";
	}

	@Override
	public void handle(Stack stack, StackEvent stackEvent) {
		String collectionName = config.getProperty(Constants.PROPERTY_KEY_STACK) + "-" + config.getProperty(Constants.PROPERTY_KEY_INSTANCE) + "-synhelp";
		
		CollectionDetail collection = ossClient.batchGetCollection(req -> req
			.names(collectionName)
		).collectionDetails().stream().findFirst().orElseThrow();
		
		if (!CollectionStatus.ACTIVE.equals(collection.status())) {
			logger.warn("Collection %s not ready, status: %s", collectionName, collection.status());
			return;
		}
				
		try (SdkHttpClient httpClient = ApacheHttpClient.builder().build()) {
			OpenSearchIndicesClient client = new OpenSearchIndicesClient(
			    new AwsSdk2Transport(
			        httpClient,
			        collection.collectionEndpoint().replace("https://", ""), 
			        "aoss",
			        Region.US_EAST_1,
			        AwsSdk2TransportOptions.builder().build()
			    )
			);
			
			boolean indexExists = client.exists(req -> req.index(IDX_NAME)).value();
			
			if (indexExists) {
				logger.info("Index %s already exists.", IDX_NAME);
				return;
			}
			
			logger.info("Index %s does not exist, creating...", IDX_NAME);
			
			client.create(req -> req
				.index(IDX_NAME)
				.settings(settings -> settings.knn(true))
				.mappings(mappings -> mappings
					.properties("text_vector", p -> p
						.knnVector(vector -> vector
							.dimension(1024)
							.method(method -> method
								.name("hnsw")
								.engine("faiss")
							)
						)
					)
					.properties("text_raw", p -> p.text(text -> text.index(true)))
					.properties("text_metadata", p -> p.text(text -> text.index(false)))
				)
			);
			
			logger.info("Index %s creation initiated...", IDX_NAME);
			
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		
	}
}
