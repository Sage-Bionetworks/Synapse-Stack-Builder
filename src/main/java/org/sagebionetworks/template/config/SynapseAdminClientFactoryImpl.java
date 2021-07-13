package org.sagebionetworks.template.config;

import org.sagebionetworks.client.RestEndpointType;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientConfig;
import org.sagebionetworks.template.Constants;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.google.inject.Inject;

public class SynapseAdminClientFactoryImpl implements SynapseAdminClientFactory {
	
	private static final int CONNECT_TIMEOUT_MS = 1000 * 60;
	private static final int SOCKET_TIMEOUT_MS = CONNECT_TIMEOUT_MS * 10;
	
	private RepoConfiguration config;
	private AWSSecretsManager secretsManager;

	@Inject
	public SynapseAdminClientFactoryImpl(RepoConfiguration config, AWSSecretsManager secretsManager) {
		this.config = config;
		this.secretsManager = secretsManager;
	}

	@Override
	public SynapseAdminClient getInstance() {
		final SimpleHttpClientConfig httpConfig = new SimpleHttpClientConfig();
		
		httpConfig.setConnectTimeoutMs(CONNECT_TIMEOUT_MS);
		httpConfig.setSocketTimeoutMs(SOCKET_TIMEOUT_MS);
		
		final SynapseAdminClientImpl client = new SynapseAdminClientImpl(httpConfig);
		
		client.setRepositoryEndpoint(getEndpoint(RestEndpointType.repo));
		client.setAuthEndpoint(getEndpoint(RestEndpointType.auth));
		client.setFileEndpoint(getEndpoint(RestEndpointType.file));
		
		final String stack = config.getProperty(Constants.PROPERTY_KEY_STACK);
		final String adminKey = getSecret(stack, Constants.SECRETS_ADMIN_KEY_ID);
		final String adminSecret = getSecret(stack, Constants.SECRETS_ADMIN_SECRET_ID);
		
		// This is a sevice call authenticated through basic auth
		client.setBasicAuthorizationCredentials(adminKey, adminSecret);
		
		return client;
	}
	
	String getSecret(String stack, String id) {
		final String stackSecretId = String.format("%s.%s", stack, id);
		return secretsManager.getSecretValue(new GetSecretValueRequest().withSecretId(stackSecretId)).getSecretString();
	}
	
	String getEndpoint(RestEndpointType type) {
		return config.getProperty(String.format(Constants.PROPERTY_KEY_CLIENT_ENDPOINT_PREFIX + ".%s", type.name()));
	}
	
}
