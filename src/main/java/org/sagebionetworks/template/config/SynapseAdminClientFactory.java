package org.sagebionetworks.template.config;

import org.sagebionetworks.client.SynapseAdminClient;

public interface SynapseAdminClientFactory {

	/**
	 * @return An instance of a synapse admin client configured with the correct credentials and endpoints according to the config
	 */
	SynapseAdminClient getInstance();

}
