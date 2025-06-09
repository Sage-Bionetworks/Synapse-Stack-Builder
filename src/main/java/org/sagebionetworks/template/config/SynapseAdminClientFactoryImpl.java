package org.sagebionetworks.template.config;

import org.sagebionetworks.client.RestEndpointType;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientConfig;
import org.sagebionetworks.template.Constants;

import com.google.inject.Inject;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

public class SynapseAdminClientFactoryImpl implements SynapseAdminClientFactory {
	
	private static final int CONNECT_TIMEOUT_MS = 1000 * 60;
	private static final int SOCKET_TIMEOUT_MS = CONNECT_TIMEOUT_MS * 10;
	
	private RepoConfiguration config;
	private SecretsManagerClient secretsManager;

	@Inject
	public SynapseAdminClientFactoryImpl(RepoConfiguration config, SecretsManagerClient secretsManager) {
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
		return secretsManager.getSecretValue(GetSecretValueRequest.builder().secretId(stackSecretId).build()).secretString();
	}
	
	String getEndpoint(RestEndpointType type) {
		return config.getProperty(String.format(Constants.PROPERTY_KEY_CLIENT_ENDPOINT_PREFIX + ".%s", type.name()));
	}
	
}
