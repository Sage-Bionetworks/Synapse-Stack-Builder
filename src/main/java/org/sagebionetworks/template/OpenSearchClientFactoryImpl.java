package org.sagebionetworks.template;

import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;

import com.google.inject.Inject;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;

public class OpenSearchClientFactoryImpl implements OpenSearchClientFactory {

	private SdkHttpClient httpClient;
	
	@Inject
	public OpenSearchClientFactoryImpl(SdkHttpClient httpClient) {
		this.httpClient = httpClient;
	}
	
	public OpenSearchIndicesClient getIndicesClient(String collectionEndpoint) {
		return new OpenSearchIndicesClient(
		    new AwsSdk2Transport(
		        httpClient,
		        collectionEndpoint.replace("https://", ""), 
		        "aoss",
		        Region.US_EAST_1,
		        AwsSdk2TransportOptions.builder().build()
		    )
		);
	}

	public OpenSearchGenericClient getDomainGenericClient(String domainEndpoint) {
		return new OpenSearchGenericClient(
		    new AwsSdk2Transport(httpClient, domainEndpoint, "es", Region.US_EAST_1,
		        AwsSdk2TransportOptions.builder().build())
		);
	}

}
