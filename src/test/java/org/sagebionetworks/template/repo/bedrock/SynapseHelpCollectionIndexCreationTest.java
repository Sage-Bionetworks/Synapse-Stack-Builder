package org.sagebionetworks.template.repo.bedrock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.config.RepoConfiguration;

import com.amazonaws.services.cloudformation.model.StackEvent;

import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;

@ExtendWith(MockitoExtension.class)
public class SynapseHelpCollectionIndexCreationTest {

	@Mock
	private LoggerFactory loggerFactory;
	
	@Mock
	private OpenSearchServerlessClient ossClient;
	
	@Mock
	private RepoConfiguration config;
	
	@InjectMocks
	private SynapseHelpCollectionIndexCreation handler;
	
	@Mock
	private StackEvent mockStackEvent;
	
	@Test
	public void testGetWaitConditionId() {
		// Call under test
		assertEquals("SynapseHelpCollectionCreateIndexWaitCondition", handler.getWaitConditionId());
	}

}
