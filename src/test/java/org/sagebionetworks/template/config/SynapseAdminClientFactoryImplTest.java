package org.sagebionetworks.template.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.template.Constants;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;

@ExtendWith(MockitoExtension.class)
public class SynapseAdminClientFactoryImplTest {
	
	@Mock
	private RepoConfiguration mockConfig;
	
	@Mock
	private AWSSecretsManager mockSecretsManager;
	
	@InjectMocks
	private SynapseAdminClientFactoryImpl factory;

	@Test
	public void testGetInstance() {
		
		when(mockConfig.getProperty(anyString())).thenReturn("http://repo-url.org", "http://auth-url.org", "http://file-url.org", "stack");
		when(mockSecretsManager.getSecretValue(any())).thenReturn(new GetSecretValueResult().withSecretString("secretKey"), new GetSecretValueResult().withSecretString("secretSecret"));
		
		// Call under test
		SynapseAdminClientImpl client = (SynapseAdminClientImpl) factory.getInstance();
		
		assertEquals("http://repo-url.org", client.getRepoEndpoint());
		assertEquals("http://auth-url.org", client.getAuthEndpoint());
		assertEquals("http://file-url.org", client.getFileEndpoint());
		
		verify(mockConfig).getProperty(Constants.PROPERTY_KEY_CLIENT_ENDPOINT_PREFIX + ".repo");
		verify(mockConfig).getProperty(Constants.PROPERTY_KEY_CLIENT_ENDPOINT_PREFIX + ".auth");
		verify(mockConfig).getProperty(Constants.PROPERTY_KEY_CLIENT_ENDPOINT_PREFIX + ".file");
		verify(mockConfig).getProperty(Constants.PROPERTY_KEY_STACK);
		
		verify(mockSecretsManager).getSecretValue(new GetSecretValueRequest().withSecretId("stack." + Constants.SECRETS_ADMIN_KEY_ID));
		verify(mockSecretsManager).getSecretValue(new GetSecretValueRequest().withSecretId("stack." + Constants.SECRETS_ADMIN_SECRET_ID));
	}

}
