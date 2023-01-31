package org.sagebionetworks.template.ip.address;

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
	@Captor
	private ArgumentCaptor<CreateOrUpdateStackRequest> requestCaptor;

	@InjectMocks
	private IpAddressPoolBuilderImpl builder;

	@BeforeEach
	public void before() {
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		builder = new IpAddressPoolBuilderImpl(mockCloudFormationClient, velocityEngine, mockConfig, mockLoggerFactory,
				mockStackTagsProvider);
	}

	@Test
	public void testBuildAndDeploy() {
		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_NLB_RECORDS_CSV))
				.thenReturn(new String[] { "one", "two" });
		when(mockConfig.getIntegerProperty(PROPERTY_KEY_IP_ADDRESS_POOL_NUMBER_AZ_PER_NLB)).thenReturn(6);
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
		// call under test
		builder.buildAndDeploy();

		verify(mockCloudFormationClient).createOrUpdateStack(requestCaptor.capture());
		CreateOrUpdateStackRequest request = requestCaptor.getValue();
		assertNotNull(request);
		assertEquals("dev-ip-address-pool", request.getStackName());
		JSONObject json = new JSONObject(request.getTemplateBody());
		JSONObject resources = json.getJSONObject("Resources");
		assertNotNull(resources);
		assertEquals(Set.of("oneAZ0", "oneAZ1", "oneAZ2", "oneAZ3", "oneAZ4", "oneAZ5", "twoAZ0", "twoAZ1", "twoAZ2",
				"twoAZ3", "twoAZ4", "twoAZ5"), resources.keySet());
		JSONObject props = resources.getJSONObject("oneAZ2").getJSONObject("Properties");
		assertEquals("[{\"Key\":\"Name\",\"Value\":\"oneAZ2\"}]", props.getJSONArray("Tags").toString());
		assertEquals("vpc", props.get("Domain"));

		assertEquals(Set.of("oneAZ0AllocationId", "oneAZ0IpAddress", "oneAZ1AllocationId", "oneAZ1IpAddress",
				"oneAZ2AllocationId", "oneAZ2IpAddress", "oneAZ3AllocationId", "oneAZ3IpAddress", "oneAZ4AllocationId",
				"oneAZ4IpAddress", "oneAZ5AllocationId", "oneAZ5IpAddress", "twoAZ0AllocationId", "twoAZ0IpAddress",
				"twoAZ1AllocationId", "twoAZ1IpAddress", "twoAZ2AllocationId", "twoAZ2IpAddress", "twoAZ3AllocationId",
				"twoAZ3IpAddress", "twoAZ4AllocationId", "twoAZ4IpAddress", "twoAZ5AllocationId", "twoAZ5IpAddress"),
				json.getJSONObject("Outputs").keySet());
	}

}
