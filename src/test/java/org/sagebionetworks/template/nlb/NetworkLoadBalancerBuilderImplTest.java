package org.sagebionetworks.template.nlb;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_IP_ADDRESS_POOL_NUMBER_AZ_PER_NLB;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_NLB_RECORDS_CSV;
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
public class NetworkLoadBalancerBuilderImplTest {

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
	private NetworkLoadBalancerBuilderImpl builder;

	@BeforeEach
	public void before() {
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		builder = new NetworkLoadBalancerBuilderImpl(mockCloudFormationClient, velocityEngine, mockConfig,
				mockLoggerFactory, mockStackTagsProvider);
	}

	@Test
	public void testBuildAndDeploy() {
		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_NLB_RECORDS_CSV))
				.thenReturn(new String[] { "www.synapse.org", "staging.synapse.org" });
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
		when(mockConfig.getIntegerProperty(PROPERTY_KEY_IP_ADDRESS_POOL_NUMBER_AZ_PER_NLB)).thenReturn(6);

		// call under test
		builder.buildAndDeploy();

		verify(mockCloudFormationClient).createOrUpdateStack(requestCaptor.capture());
		CreateOrUpdateStackRequest request = requestCaptor.getValue();
		assertNotNull(request);
		assertEquals("dev-nlbs", request.getStackName());
		JSONObject json = new JSONObject(request.getTemplateBody());
		JSONObject resources = json.getJSONObject("Resources");
		assertNotNull(resources);
		assertEquals(Set.of("wwwsynapseorg", "stagingsynapseorg"), resources.keySet());
		JSONObject props = resources.getJSONObject("wwwsynapseorg").getJSONObject("Properties");
		assertEquals("[{\"Key\":\"load_balancing.cross_zone.enabled\",\"Value\":\"true\"}]", props.getJSONArray("LoadBalancerAttributes").toString());
		assertEquals("www-synapse-org", props.get("Name"));
		JSONArray subnetMapping = props.getJSONArray("SubnetMappings");
		assertEquals(6, subnetMapping.length());
		assertEquals(
				"{\"AllocationId\":{\"Fn::ImportValue\":\"us-east-1-dev-ip-address-pool-wwwsynapseorgAZ0-AllocationId\"}"
						+ ",\"SubnetId\":{\"Fn::Select\":[0,{\"Fn::Split\":[\", \","
						+ "{\"Fn::ImportValue\":\"us-east-1-synapse-dev-vpc-2-public-subnets-Public-Subnets\"}]}]}}",
				subnetMapping.get(0).toString());

		assertEquals(Set.of("wwwsynapseorgarn", "stagingsynapseorgarn"), json.getJSONObject("Outputs").keySet());
	}
}
