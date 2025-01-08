package org.sagebionetworks.template;

import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;

public interface OpenSearchClientFactory {

	OpenSearchIndicesClient getIndicesClient(String collectionEndpoint);
	
}
