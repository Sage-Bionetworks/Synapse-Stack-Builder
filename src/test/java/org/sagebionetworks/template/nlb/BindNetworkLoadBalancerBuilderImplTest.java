package org.sagebionetworks.template.nlb;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BIND_RECORD_TO_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;

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
	public void testBuildMappingWithDependencies() {
		List<RecordToStackMapping> old = List.of(
				RecordToStackMapping.builder().withMapping("www.synapse.org->portal-dev-101-0").build(),
				RecordToStackMapping.builder().withMapping("staging.synapse.org->portal-dev-102-0").build(),
				RecordToStackMapping.builder().withMapping("tst.synapse.org->portal-dev-103-0").build()
		);
		List<RecordToStackMapping> neu = List.of(
				RecordToStackMapping.builder().withMapping("www.synapse.org->portal-dev-102-0").build(),
				RecordToStackMapping.builder().withMapping("staging.synapse.org->portal-dev-103-0").build(),
				RecordToStackMapping.builder().withMapping("tst.synapse.org->portal-dev-104-0").build()
		);

		List<RecordToStackMapping> result = BindNetworkLoadBalancerBuilderImpl.buildMappingWithDependencies(old, neu);
		List<RecordToStackMapping> expected = List.of(
				RecordToStackMapping.builder().withMapping("www.synapse.org->portal-dev-102-0").withDependsOn("staging.synapse.org").build(),
				RecordToStackMapping.builder().withMapping("staging.synapse.org->portal-dev-103-0").withDependsOn("tst.synapse.org").build(),
				RecordToStackMapping.builder().withMapping("tst.synapse.org->portal-dev-104-0").build()
		);
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildMappingWithDependenciesWithNoChange() {
		List<RecordToStackMapping> old = List.of(
				RecordToStackMapping.builder().withMapping("www.synapse.org->portal-dev-101-0").build(),
				RecordToStackMapping.builder().withMapping("staging.synapse.org->portal-dev-102-0").build(),
				RecordToStackMapping.builder().withMapping("tst.synapse.org->portal-dev-103-0").build()
		);
		List<RecordToStackMapping> neu = List.of(
				RecordToStackMapping.builder().withMapping("www.synapse.org->portal-dev-101-0").build(),
				RecordToStackMapping.builder().withMapping("staging.synapse.org->portal-dev-103-0").build(),
				RecordToStackMapping.builder().withMapping("tst.synapse.org->portal-dev-104-0").build()
		);

		List<RecordToStackMapping> result = BindNetworkLoadBalancerBuilderImpl.buildMappingWithDependencies(old, neu);
		List<RecordToStackMapping> expected = List.of(
				RecordToStackMapping.builder().withMapping("www.synapse.org->portal-dev-101-0").build(),
				RecordToStackMapping.builder().withMapping("staging.synapse.org->portal-dev-103-0").withDependsOn("tst.synapse.org").build(),
				RecordToStackMapping.builder().withMapping("tst.synapse.org->portal-dev-104-0").build()
		);
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildStackMapping() {
		Stack stack = new Stack()
				.withOutputs(new Output().withOutputKey(BindNetworkLoadBalancerBuilderImpl.MAPPINGS_CSV)
						.withOutputValue("www.synapse.org->portal-dev-101-0,staging.synapse.org->portal-dev-103-0"));
		// call under test
		List<RecordToStackMapping> result = BindNetworkLoadBalancerBuilderImpl.buildStackMapping(stack);
		List<RecordToStackMapping> expected = List.of(
				RecordToStackMapping.builder().withMapping("www.synapse.org->portal-dev-101-0").build(),
				RecordToStackMapping.builder().withMapping("staging.synapse.org->portal-dev-103-0").build()
		);
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildStackMappingWithSingleValue() {
		Stack stack = new Stack()
				.withOutputs(new Output().withOutputKey(BindNetworkLoadBalancerBuilderImpl.MAPPINGS_CSV)
						.withOutputValue("staging.synapse.org->portal-dev-103-0"));
		// call under test
		List<RecordToStackMapping> result = BindNetworkLoadBalancerBuilderImpl.buildStackMapping(stack);
		List<RecordToStackMapping> expected = List.of(
				RecordToStackMapping.builder().withMapping("staging.synapse.org->portal-dev-103-0").build()
		);
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildStackMappingWithOtherKey() {
		Stack stack = new Stack()
				.withOutputs(new Output().withOutputKey("otherKey")
						.withOutputValue("not the value you are looking for"));
		// call under test
		List<RecordToStackMapping> result = BindNetworkLoadBalancerBuilderImpl.buildStackMapping(stack);
		List<RecordToStackMapping> expected = Collections.emptyList();
		assertEquals(expected, result);
	}

	@Test
	public void testBuildAndDeploy() {
		
		// stack does not yet exist
		when(mockCloudFormationClient.describeStack(any())).thenReturn(Optional.empty());
		
		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_BIND_RECORD_TO_STACK))
				.thenReturn(new String[] { "www.sagebase.org->portal-dev-123-4", "dev.sagebase.org->repo-dev-123-5" });
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");

		// call under test
		builder.buildAndDeploy();

		verify(mockCloudFormationClient).createOrUpdateStack(requestCaptor.capture());
		CreateOrUpdateStackRequest request = requestCaptor.getValue();
		assertNotNull(request);
		assertEquals("dev-dns-record-to-stack-mapping", request.getStackName());
		JSONObject json = new JSONObject(request.getTemplateBody());
		JSONObject resources = json.getJSONObject("Resources");
		assertNotNull(resources);
		assertEquals(Set.of("wwwsagebaseorg80", "wwwsagebaseorg443", "devsagebaseorg80", "devsagebaseorg443"),
				resources.keySet());
		JSONObject props = resources.getJSONObject("wwwsagebaseorg443").getJSONObject("Properties");
		assertEquals("[{\"Type\":\"forward\","
				+ "\"TargetGroupArn\":{\"Fn::ImportValue\":\"us-east-1-portal-dev-123-4-443-alb-target-group\"}}]",
				props.getJSONArray("DefaultActions").toString());
		assertEquals("{\"Fn::ImportValue\":\"us-east-1-dev-nlbs-www-sagebase-org-nlb-arn\"}",
				props.getJSONObject("LoadBalancerArn").toString());
		JSONObject outputs = json.getJSONObject("Outputs");
		assertNotNull(outputs);
		assertEquals(Set.of("mappingsCSV"), outputs.keySet());
		assertEquals("www.sagebase.org->portal-dev-123-4,dev.sagebase.org->repo-dev-123-5", outputs.getJSONObject("mappingsCSV").get("Value"));
		
		verify(mockCloudFormationClient).describeStack("dev-dns-record-to-stack-mapping");
	}
	
	@Test
	public void testBuildAndDeployWithExistingStack() {
		
		// setup an existing stack
		when(mockCloudFormationClient.describeStack(any())).thenReturn(Optional.of(new Stack()
				.withOutputs(new Output().withOutputKey(BindNetworkLoadBalancerBuilderImpl.MAPPINGS_CSV)
						.withOutputValue("www.synapse.org->portal-dev-101-0,staging.synapse.org->portal-dev-102-0"))));
		
		when(mockConfig.getComaSeparatedProperty(PROPERTY_KEY_BIND_RECORD_TO_STACK))
				.thenReturn(new String[] { "www.synapse.org->portal-dev-102-0", "staging.synapse.org->portal-dev-103-0" });
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
		
		// call under test
		builder.buildAndDeploy();

		verify(mockCloudFormationClient).createOrUpdateStack(requestCaptor.capture());
		CreateOrUpdateStackRequest request = requestCaptor.getValue();
		assertNotNull(request);
		assertEquals("dev-dns-record-to-stack-mapping", request.getStackName());
		JSONObject json = new JSONObject(request.getTemplateBody());
		System.out.println(json.toString(5));
		JSONObject resources = json.getJSONObject("Resources");
		assertNotNull(resources);
		assertEquals(Set.of("wwwsynapseorg80","wwwsynapseorg443","stagingsynapseorg80", "stagingsynapseorg443"),
				resources.keySet());
		
		// www depends on staging
		assertEquals("[\"stagingsynapseorg80\"]", resources.getJSONObject("wwwsynapseorg80").get("DependsOn").toString());
		assertEquals("[\"stagingsynapseorg443\"]", resources.getJSONObject("wwwsynapseorg443").get("DependsOn").toString());
		
		// staging has no dependencies
		assertFalse(resources.getJSONObject("stagingsynapseorg80").has("DependsOn"));
		assertFalse(resources.getJSONObject("stagingsynapseorg443").has("DependsOn"));
		
		verify(mockCloudFormationClient).describeStack("dev-dns-record-to-stack-mapping");
		
	}
}
