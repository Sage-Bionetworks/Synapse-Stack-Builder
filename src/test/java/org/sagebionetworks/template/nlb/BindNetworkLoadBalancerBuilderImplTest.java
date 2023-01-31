package org.sagebionetworks.template.nlb;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BIND_RECORD_TO_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.Configuration;

@ExtendWith(MockitoExtension.class)
public class BindNetworkLoadBalancerBuilderImplTest {

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

	@Captor
	private ArgumentCaptor<CreateOrUpdateStackRequest> requestCaptor;

	@InjectMocks
	private BindNetworkLoadBalancerBuilderImpl builder;

	@BeforeEach
	public void before() {
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		builder = new BindNetworkLoadBalancerBuilderImpl(mockCloudFormationClient, velocityEngine, mockConfig,
				mockLoggerFactory, mockStackTagsProvider);
	}

	@Test
	public void testBuildAndDeploy() {
		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_BIND_RECORD_TO_STACK))
				.thenReturn(new String[] { "www.sagebase.org->portal-dev-123-4", "dev.sagebase.org->repo-dev-123-5" });
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");

		// call under test
		builder.buildAndDeploy();

		verify(mockCloudFormationClient).createOrUpdateStack(requestCaptor.capture());
		CreateOrUpdateStackRequest request = requestCaptor.getValue();
		assertNotNull(request);
		assertEquals("dns-record-to-stack-mapping", request.getStackName());
		JSONObject json = new JSONObject(request.getTemplateBody());
		JSONObject resources = json.getJSONObject("Resources");
		assertNotNull(resources);
		assertEquals(Set.of("wwwsagebaseorg80", "wwwsagebaseorg443", "devsagebaseorg80", "devsagebaseorg443"),
				resources.keySet());
		JSONObject props = resources.getJSONObject("wwwsagebaseorg443").getJSONObject("Properties");
		assertEquals("[{\"Type\":\"forward\","
				+ "\"TargetGroupArn\":{\"Fn::ImportValue\":\"us-east-1-portal-dev-123-4-443-alb-target-group\"}}]", props.getJSONArray("DefaultActions").toString());
		assertEquals("{\"Fn::ImportValue\":\"us-east-1-dev-nlbs-www-sagebase-org-nlb-arn\"}", props.getJSONObject("LoadBalancerArn").toString());
	}
}
