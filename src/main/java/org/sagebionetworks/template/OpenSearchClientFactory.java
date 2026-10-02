package org.sagebionetworks.template;

import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;

public interface OpenSearchClientFactory {

	OpenSearchIndicesClient getIndicesClient(String collectionEndpoint);

	/**
	 * @param domainEndpoint host of a managed OpenSearch domain, without a scheme.
	 */
	OpenSearchGenericClient getDomainGenericClient(String domainEndpoint);
	
}
