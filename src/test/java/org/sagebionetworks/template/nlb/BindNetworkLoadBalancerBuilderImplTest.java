package org.sagebionetworks.template.nlb;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BIND_RECORD_TO_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.app.VelocityEngine;
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
	public void testBuildAndDeploy() throws InterruptedException {

		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_BIND_RECORD_TO_STACK))
				.thenReturn(new String[] { "www.sagebase.org->portal-dev-123-4", "dev.sagebase.org->repo-dev-123-5",
						"staging.synapse.org->none" });
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");

		// call under test
		builder.buildAndDeploy();

		verify(mockCloudFormationClient).createOrUpdateStack(requestCaptor.capture());
		verify(mockCloudFormationClient).waitForStackToComplete("dev-dns-record-to-stack-mapping");
		
		CreateOrUpdateStackRequest request = requestCaptor.getValue();
		assertNotNull(request);
		assertEquals("dev-dns-record-to-stack-mapping", request.getStackName());
		JSONObject json = new JSONObject(request.getTemplateBody());
		System.out.println(json.toString(5));

		JSONObject resources = json.getJSONObject("Resources");
		assertNotNull(resources);
		assertEquals(Set.of("wwwsagebaseorg80t", "wwwsagebaseorg80l", "wwwsagebaseorg443t", "wwwsagebaseorg443l",
				"devsagebaseorg80t", "devsagebaseorg80l", "devsagebaseorg443t", "devsagebaseorg443l",
				"stagingsynapseorg80t", "stagingsynapseorg80l", "stagingsynapseorg443t", "stagingsynapseorg443l"),
				resources.keySet());

		// properties of the first target
		JSONObject props = resources.getJSONObject("wwwsagebaseorg80t").getJSONObject("Properties");
		assertEquals("/", props.get("HealthCheckPath"));
		assertEquals(80, props.get("HealthCheckPort"));
		assertEquals("HTTP", props.get("HealthCheckProtocol"));
		
		assertEquals("[{\"Key\":\"record\",\"Value\":\"www-sagebase-org-80\"}]", props.getJSONArray("Tags").toString());
		assertEquals(80, props.get("Port"));
		assertEquals("ipv4", props.get("IpAddressType"));
		assertEquals("TCP", props.get("Protocol"));
		assertEquals("[{\"Id\":{\"Fn::ImportValue\":\"us-east-1-portal-dev-123-4-alb-arn\"},\"Port\":80}]",
				props.getJSONArray("Targets").toString());
		assertEquals("alb", props.get("TargetType"));
		assertEquals("{\"Fn::ImportValue\":\"us-east-1-synapse-dev-vpc-2-VPCId\"}", props.getJSONObject("VpcId").toString());

		// properties of the first listener
		props = resources.getJSONObject("wwwsagebaseorg443l").getJSONObject("Properties");
		
		assertEquals("[{\"Type\":\"forward\",\"TargetGroupArn\":{\"Ref\":\"wwwsagebaseorg443t\"}}]",
				props.getJSONArray("DefaultActions").toString());
		assertEquals("{\"Fn::ImportValue\":\"us-east-1-dev-nlbs-www-sagebase-org-nlb-arn\"}",
				props.getJSONObject("LoadBalancerArn").toString());
		JSONObject outputs = json.getJSONObject("Outputs");
		assertNotNull(outputs);
		assertEquals(Set.of("mappingsCSV"), outputs.keySet());
		assertEquals("www.sagebase.org->portal-dev-123-4,dev.sagebase.org->repo-dev-123-5,staging.synapse.org->none",
				outputs.getJSONObject("mappingsCSV").get("Value"));
		
		// when the target is set to 'none' the target group should exist but it should not have a 'Targets' property.
		props = resources.getJSONObject("stagingsynapseorg443t").getJSONObject("Properties");
		assertEquals("/", props.get("HealthCheckPath"));
		assertEquals(443, props.get("HealthCheckPort"));
		assertEquals("HTTPS", props.get("HealthCheckProtocol"));
		
		assertEquals("[{\"Key\":\"record\",\"Value\":\"staging-synapse-org-443\"}]", props.getJSONArray("Tags").toString());
		assertEquals(443, props.get("Port"));
		assertEquals("ipv4", props.get("IpAddressType"));
		assertEquals("TCP", props.get("Protocol"));
		assertFalse(props.has("Targets"));
		assertEquals("alb", props.get("TargetType"));
		assertEquals("{\"Fn::ImportValue\":\"us-east-1-synapse-dev-vpc-2-VPCId\"}", props.getJSONObject("VpcId").toString());

	}
}
