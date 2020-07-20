package org.sagebionetworks.template.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.amazonaws.services.cloudformation.model.Tag;
import org.apache.logging.log4j.core.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.*;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class WebACLBuilderImplTest {

	@Mock
	CloudFormationClient mockCloudFormationClient;
	@Mock
	Configuration config;
	@Mock
	AmazonElasticLoadBalancing mockElbClient;
	@Mock
	LoggerFactory mockLoggerFactory;
	@Mock
	Logger mockLogger;
	@Mock
	StackTagsProvider mockStackTagsProvider;

	@Captor
	ArgumentCaptor<DescribeLoadBalancersRequest> describeRequestCaptor;
	@Captor
	ArgumentCaptor<CreateOrUpdateStackRequest> createOrUpdateStackRequestCaptor;

	VelocityEngine velocityEngine;
	WebACLBuilderImpl builder;

	String stack;
	String instance;

	List<String> environmentNames;
	List<Stack> stacks;
	List<String> endpointUrls;

	List<Tag> expectedTags;

	@Before
	public void before() throws InterruptedException {
		// use a real velocity engine
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();

		expectedTags = new LinkedList<>();
		Tag t = new Tag().withKey("aKey").withValue("aValue");
		expectedTags.add(t);
		when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);

		stack = "dev";
		instance = "101";

		when(config.getProperty(PROPERTY_KEY_STACK)).thenReturn("dev");
		when(config.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn("101");
		
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);

		builder = new WebACLBuilderImpl(mockCloudFormationClient, velocityEngine, config, mockElbClient, mockLoggerFactory, mockStackTagsProvider);
		
		environmentNames = new LinkedList<>();
		stacks = new LinkedList<>();
		endpointUrls = new LinkedList<>();
		for(EnvironmentType type: EnvironmentType.values()) {
			String stackName = type.getShortName()+"-"+stack+"-"+instance+"-0";
			environmentNames.add(stackName);
			String loadBalancerName = "awseb-AWSEB-"+type.getShortName();
			String endpointUrl = loadBalancerName+"-717445046.us-east-1.elb.amazonaws.com";
			endpointUrls.add(endpointUrl);
			
			Stack stack = new Stack();
			stack.withStackName(stackName);
			
			Output output = new Output().withExportName("EndpointURL")
					.withOutputValue(endpointUrl);
			stack.withOutputs(output);
			stacks.add(stack);

			String loadBalancerArn = type.getShortName()+"-load-balancer-ARN";
			LoadBalancer loadBalancer = new LoadBalancer().withLoadBalancerArn(loadBalancerArn);
			DescribeLoadBalancersResult describeResults = new DescribeLoadBalancersResult().withLoadBalancers(loadBalancer);

			when(mockElbClient.describeLoadBalancers(new DescribeLoadBalancersRequest().withNames(loadBalancerName))).thenReturn(describeResults);
			
			when(mockCloudFormationClient.waitForStackToComplete(stackName)).thenReturn(stack);
		}
		
	}
	
	@Test
	public void testCreateStackName() {
		String name = builder.createStackName();
		assertEquals("dev-101-web-acl", name);
	}

	@Test
	public void testGetTypeFromStackName() {
		// call under test
		EnvironmentType type = builder.getTypeFromStackName("workers-dev-hill-0");
		assertEquals(EnvironmentType.REPOSITORY_WORKERS, type);
	}

	@Test
	public void testGetEndpointUrlFromStack() {
		// call under test
		String endpoint = builder.getEndpointUrlFromStack(stacks.get(1));
		assertEquals("awseb-AWSEB-workers-717445046.us-east-1.elb.amazonaws.com", endpoint);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetEndpointUrlFromStackNullOutputs() {
		Stack stack = stacks.get(1);
		stack.setOutputs(null);
		// call under test
		builder.getEndpointUrlFromStack(stack);
	}

	@Test
	public void testGetLoadBalancerNameFromUrl() {
		// call under test
		String albName = builder.getLoadBalancerNameFromUrl(endpointUrls.get(0));
		assertEquals("awseb-AWSEB-repo", albName);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetLoadBalancerNameFromUrlNoParse() {
		// call under test
		builder.getLoadBalancerNameFromUrl("not-an-endpoint");
	}

	@Test
	public void testaddApplicationLoadBalanverArnToContext() {
		VelocityContext context = new VelocityContext();
		Stack stack = stacks.get(1);
		// call under test
		builder.addApplicationLoadBalanverArnToContext(context, stack);
		assertEquals("workers-load-balancer-ARN", context.get("workersLoadBalancerARN"));
		verify(mockElbClient).describeLoadBalancers(describeRequestCaptor.capture());
		assertEquals(Lists.newArrayList("awseb-AWSEB-workers"), describeRequestCaptor.getValue().getNames());
	}

	@Test (expected=IllegalArgumentException.class)
	public void testaddApplicationLoadBalanverArnToContextNoMatch() {
		Stack stack = stacks.get(1);
		// setup no results
		when(mockElbClient.describeLoadBalancers(any(DescribeLoadBalancersRequest.class)))
				.thenReturn(new DescribeLoadBalancersResult());
		VelocityContext context = new VelocityContext();
		// call under test
		builder.addApplicationLoadBalanverArnToContext(context, stack);
	}
	
	@Test
	public void testCreateContext() throws InterruptedException {
		// call under test
		VelocityContext context = builder.createContext(environmentNames);
		assertNotNull(context);
		assertEquals(stack, context.get(STACK));
		assertEquals(instance, context.get(INSTANCE));
		assertEquals("portal-load-balancer-ARN", context.get("portalLoadBalancerARN"));
		assertEquals("workers-load-balancer-ARN", context.get("workersLoadBalancerARN"));
		assertEquals("repo-load-balancer-ARN", context.get("repoLoadBalancerARN"));
		verify(mockCloudFormationClient, times(3)).waitForStackToComplete(any(String.class));
	}
	
	@Test
	public void testBuildWebACL() {
		// call under test
		builder.buildWebACL(environmentNames);
		verify(mockCloudFormationClient).createOrUpdateStack(createOrUpdateStackRequestCaptor.capture());
		CreateOrUpdateStackRequest request= createOrUpdateStackRequestCaptor.getValue();
		assertNotNull(request);
		assertEquals("dev-101-web-acl", request.getStackName());
		JSONObject template = new JSONObject(request.getTemplateBody());
		assertNotNull(template);
		assertEquals(expectedTags, request.getTags());
		assertEquals(false, request.getEnableTerminationProtection());
		//System.out.println(template.toString(5));
		verify(mockLogger, times(2)).info(any(String.class));
	}

	@Test
	public void testBuildWebACL__emptyList() {
		// call under test
		builder.buildWebACL(Collections.emptyList());
		verifyZeroInteractions(mockCloudFormationClient);
	}


}
