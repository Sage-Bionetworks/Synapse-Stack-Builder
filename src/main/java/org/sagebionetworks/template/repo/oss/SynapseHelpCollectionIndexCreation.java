package org.sagebionetworks.template.repo.oss;

import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.WaitConditionHandler;

import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.google.inject.Inject;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

public class SynapseHelpCollectionIndexCreation implements WaitConditionHandler {
	
	private static final String IDX_NAME = "vector-idx";
	
	private Logger logger;
	
	@Inject
	public SynapseHelpCollectionIndexCreation(LoggerFactory loggerFactory) {
		this.logger = loggerFactory.getLogger(SynapseHelpCollectionIndexCreation.class);
	}
	
	@Override
	public String getWaitConditionId() {
		return "SynapseHelpCollectionCreateIndexCondition";
	}
	
	@Override
	public String getSignalId() {
		return "vector-index-created";
	}

	@Override
	public void handle(Stack stack, StackEvent stackEvent) {

		String collectionEndpoint = stack.getOutputs().stream()
			.filter( output -> output.getOutputKey().equals("SynapseHelpCollectionEndpoint"))
			.findFirst()
			.orElseThrow()
			.getOutputValue();
		
		
		try (SdkHttpClient httpClient = ApacheHttpClient.builder().build()) {
			OpenSearchIndicesClient client = new OpenSearchIndicesClient(
			    new AwsSdk2Transport(
			        httpClient,
			        collectionEndpoint.replace("https://", ""), 
			        "aoss",
			        Region.US_EAST_1,
			        AwsSdk2TransportOptions.builder().build()
			    )
			);
			
			boolean indexExists = client.exists(req -> req.index(IDX_NAME)).value();
			
			if (indexExists) {
				logger.info("Index " + IDX_NAME + " already exists.");
				return;
			}
			
			logger.info("Index " + IDX_NAME + " does not exist, creating...");
			
			client.create(req -> req
				.index(IDX_NAME)
				.settings(settings -> settings.knn(true))
				.mappings(mappings -> mappings
					.properties("text_vector", p -> p.knnVector(vector -> vector.dimension(1024)))
					.properties("text_raw", p -> p.text(text -> text.index(true)))
					.properties("text_metadata", p -> p.text(text -> text.index(false)))
				)
			);
			
			logger.info("Index " + IDX_NAME + " creation initiated...");
			
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		
	}
}
