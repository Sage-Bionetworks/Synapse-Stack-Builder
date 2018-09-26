package org.sagebionetworks.template.repo;

import static org.sagebionetworks.template.Constants.INSTANCE;
import static org.sagebionetworks.template.Constants.JSON_INDENT;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.STACK;

import java.io.StringWriter;
import java.util.List;
import java.util.StringJoiner;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.Configuration;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.google.inject.Inject;

public class WebACLBuilderImpl implements WebACLBuilder {

	public static final String TEMPLATES_REPO_WEB_ACL_TEMPLATE_JSON = "/templates/repo/web-acl-template.json";
	CloudFormationClient cloudFormationClient;
	VelocityEngine velocityEngine;
	Configuration config;
	AmazonElasticLoadBalancing elbClient;
	Logger logger;

	@Inject
	public WebACLBuilderImpl(CloudFormationClient cloudFormationClient, VelocityEngine velocityEngine, Configuration config, AmazonElasticLoadBalancing elbClient, LoggerFactory loggerFactory) {
		super();
		this.cloudFormationClient = cloudFormationClient;
		this.velocityEngine = velocityEngine;
		this.config = config;
		this.elbClient = elbClient;
		this.logger = loggerFactory.getLogger(WebACLBuilderImpl.class);
	}

	@Override
	public void buildWebACL(List<String> environmentNames) {
		if (environmentNames.isEmpty()){
			return;
		}

		VelocityContext context = createContext(environmentNames);
		// Merge the context with the template
		Template template = this.velocityEngine.getTemplate(TEMPLATES_REPO_WEB_ACL_TEMPLATE_JSON);
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		// Parse the resulting template
		String resultJSON = stringWriter.toString();
		JSONObject templateJson = new JSONObject(resultJSON);
		// Format the JSON
		resultJSON = templateJson.toString(JSON_INDENT);
		String stackName = createStackName();
		this.logger.info("Template for stack: " + stackName);
		this.logger.info(resultJSON);
		// create or update the template
		this.cloudFormationClient.createOrUpdateStack(new CreateOrUpdateStackRequest().withStackName(stackName)
				.withTemplateBody(resultJSON));
	}
	
	/**
	 * Create the name of the stack from the input.
	 * 
	 * @return
	 */
	String createStackName() {
		StringJoiner joiner = new StringJoiner("-");
		joiner.add(config.getProperty(PROPERTY_KEY_STACK));
		joiner.add(config.getProperty(PROPERTY_KEY_INSTANCE));
		joiner.add("web-acl");
		return joiner.toString();
	}
	
	
	VelocityContext createContext(List<String> environmentNames) {
		VelocityContext context = new VelocityContext();
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		String instance = config.getProperty(PROPERTY_KEY_INSTANCE);
		context.put(STACK, stack);
		context.put(INSTANCE, instance);
		// add the ALB ARNs to the context
		addApplicationLoadBalancerARNsToContext(context, environmentNames);
		return context;
	}
	
	void addApplicationLoadBalancerARNsToContext(VelocityContext context, List<String> environmentNames) {
		// Wait for each environment 
		for(String environmentName: environmentNames) {
			try {
				Stack stack = cloudFormationClient.waitForStackToComplete(environmentName);
				// lookup and add the ALB ARN
				addApplicationLoadBalanverArnToContext(context, stack);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Lookup the ARN of the ALB for the given stack.
	 * @param context
	 * @param stack
	 */
	void addApplicationLoadBalanverArnToContext(VelocityContext context, Stack stack) {
		EnvironmentType type = getTypeFromStackName(stack.getStackName());
		String albUrl = getEndpointUrlFromStack(stack);
		String albName = getLoadBalancerNameFromUrl(albUrl);
		// Lookup the ARN
		DescribeLoadBalancersResult result = elbClient.describeLoadBalancers(new DescribeLoadBalancersRequest().withNames(albName));
		if(result.getLoadBalancers() == null || result.getLoadBalancers().size() != 1) {
			throw new IllegalArgumentException("Cannot find a Load Balancer with the name: "+albName);
		}
		String albArn = result.getLoadBalancers().get(0).getLoadBalancerArn();
		// add the load balancer ARN to the context
		context.put(type.getShortName()+"LoadBalancerARN", albArn);
	}
	
	/**
	 * Get the a Loadbalancer's name give its URL.
	 * 
	 * @param url
	 * @return
	 */
	String getLoadBalancerNameFromUrl(String url) {
		String[] split = url.split("-");
		if(split.length != 6) {
			throw new IllegalArgumentException("Cannot parse load balancer URL: "+url);
		}
		StringJoiner joiner = new StringJoiner("-");
		for(int i=0; i<3; i++) {
			joiner.add(split[i]);
		}
		return joiner.toString();
	}
	
	/**
	 * Extract the endpoint URL fr m the passed stack.
	 * @param stack
	 * @return
	 */
	String getEndpointUrlFromStack(Stack stack) {
		if(stack.getOutputs() != null) {
			for(Output output: stack.getOutputs()) {
				if(output.getExportName().contains("EndpointURL")) {
					return output.getOutputValue();
				}
			}
		}
		throw new IllegalArgumentException("Unable to find 'EndpointURL' in the output from stack: "+stack.getStackName());
	}
	
	/**
	 * Get the EnvironmentType from the given stack name.
	 * @param stackName
	 * @return
	 */
	EnvironmentType getTypeFromStackName(String stackName) {
		String[] split = stackName.split("-");
		return EnvironmentType.valueOfPrefix(split[0]);
	}

}
