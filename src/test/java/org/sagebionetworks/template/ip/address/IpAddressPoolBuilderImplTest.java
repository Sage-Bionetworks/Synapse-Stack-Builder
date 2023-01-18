package org.sagebionetworks.template.ip.address;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.ip.address.IpAddressPoolBuilderImpl;

import static org.sagebionetworks.template.Constants.*;

@ExtendWith(MockitoExtension.class)
public class IpAddressPoolBuilderImplTest {

	@Mock
	private Configuration mockConfig;
	@Mock
	private CloudFormationClient mockCloudFormationClient;

	private VelocityEngine velocityEngine = new TemplateGuiceModule().velocityEngineProvider();
	@Mock
	private LoggerFactory mockLoggerFactory;
	@Mock
	private Logger mockLogger;
	@Mock
	private StackTagsProvider mockStackTagsProvider;

	@InjectMocks
	private IpAddressPoolBuilderImpl builder;
	
	@BeforeEach
	public void before() {
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		builder = new IpAddressPoolBuilderImpl(mockCloudFormationClient, velocityEngine, mockConfig, mockLoggerFactory, mockStackTagsProvider);
	}

	@Test
	public void testBuildAndDeploy() {
		when(mockConfig.getProperty(PROPERTY_KEY_IP_ADDRESS_POOL_NUMBER_NLB)).thenReturn("2");
		when(mockConfig.getProperty(PROPERTY_KEY_IP_ADDRESS_POOL_NUMBER_AZ_PER_NLB)).thenReturn("6");
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
		// call under test
		builder.buildAndDeploy();
	}

}
