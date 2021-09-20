package org.sagebionetworks.template.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROD_STACK_NAME;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.config.RepoConfiguration;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManager;

@ExtendWith(MockitoExtension.class)
public class SynapseDocsBuilderImplTest {
	
	@Mock
	TransferManager mockTransferManager;
	
	@Mock
	AmazonS3 mockS3Client;
	
	@Mock
	RepoConfiguration mockConfig;
	
	String prodInstance;
	
	SynapseDocsBuilderImpl builder;
	
	@BeforeEach
	public void before() {
		prodInstance = "123";
		builder = new SynapseDocsBuilderImpl(mockS3Client, mockConfig);
	}
	
	@AfterEach
	public void after() {
		return;
	}
	
	@Test
	public void testDeployDocsWithNonProdStack() {
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("notProd");
		assertFalse(builder.deployDocs());
	}
	
	/*
	@Test
	public void testDeployDocsWithUpToDateDocs() {
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(PROD_STACK_NAME);
		when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn("123");
	}
	*/
}
