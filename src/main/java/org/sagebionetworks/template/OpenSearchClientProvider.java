package org.sagebionetworks.template;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

public class OpenSearchClientProvider {

	private SdkHttpClient httpClient;
	
	public OpenSearchClientProvider() {
		this.httpClient = ApacheHttpClient.builder().build();
	}
	
	public OpenSearchClient getOpenSearchClient(String collectionEndpoint) {
		return new OpenSearchClient(
		    new AwsSdk2Transport(
		        httpClient,
		        collectionEndpoint.replace("https://", ""), 
		        "aoss",
		        Region.US_EAST_1,
		        AwsSdk2TransportOptions.builder().build()
		    )
		);
	}
 
}
