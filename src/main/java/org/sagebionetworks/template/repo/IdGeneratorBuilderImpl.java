package org.sagebionetworks.template.repo;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.RdsClientWrapper;
import org.sagebionetworks.template.repo.beanstalk.SecretBuilder;

import com.google.inject.Inject;
import software.amazon.awssdk.services.cloudformation.model.Parameter;

import static org.sagebionetworks.template.Constants.*;

public class IdGeneratorBuilderImpl implements IdGeneratorBuilder {

	CloudFormationClientWrapper cloudFormationClientWrapper;
	VelocityEngine velocityEngine;
	Configuration config;
	Logger logger;
	SecretBuilder secretBuilder;
	RdsClientWrapper rdsClientWrapper;

	@Inject
	public IdGeneratorBuilderImpl(CloudFormationClientWrapper cloudFormationClientWrapper, VelocityEngine velocityEngine,
                                  Configuration config, LoggerFactory loggerFactory, SecretBuilder secretBuilder,
                                  RdsClientWrapper rdsClientWrapper) {
		super();
		this.cloudFormationClientWrapper = cloudFormationClientWrapper;
		this.velocityEngine = velocityEngine;
		this.config = config;
		this.logger = loggerFactory.getLogger(IdGeneratorBuilderImpl.class);
		this.secretBuilder = secretBuilder;
		this.rdsClientWrapper = rdsClientWrapper;
	}

	@Override
	public void buildAndDeploy() {
		VelocityContext context = new VelocityContext();
		String color = config.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR);
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		String hostedZoneId = config.getProperty(PROPERTY_KEY_ID_GENERATOR_HOSTED_ZONE_ID);
		String databaseIdentifier = stack+"-id-generator-db-4-"+color.toLowerCase();
		context.put(STACK, stack);
		context.put(GLOBAL_RESOURCES_EXPORT_PREFIX, Constants.createGlobalResourcesExportPrefix(stack));
		context.put(VPC_EXPORT_PREFIX, Constants.createVpcExportPrefix(stack));
		context.put(VPC_SUBNET_COLOR, color);
		context.put(DATABASE_IDENTIFIER, databaseIdentifier);
		context.put(HOSTED_ZONE, hostedZoneId);
		context.put(DB_INSTANCE_CLASS, ID_GENERATOR_DB_INSTANCE_CLASS);
		// This database is Multi-AZ, so its subnet group must exclude any zone that cannot host the
		// instance class, otherwise RDS silently leaves it single-AZ. See PLFM-9965.
		context.put(DATABASE_SUBNETS, getDatabaseSubnets(stack, color));

		Parameter parameter = Parameter.builder()
				.parameterKey(Constants.PARAMETER_MYSQL_PASSWORD)
				.parameterValue(secretBuilder.getIdGeneratorPassword())
				.build();

		// Merge the context with the template
		Template template = this.velocityEngine.getTemplate(TEMPLATE_ID_GENERATOR);
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		// Parse the resulting template
		String resultJSON = stringWriter.toString();
		JSONObject templateJson = new JSONObject(resultJSON);
		// Format the JSON
		resultJSON = templateJson.toString(JSON_INDENT);
		System.out.println(resultJSON);
		String stackName = stack + "-id-generator-4-"+color.toLowerCase();
		this.logger.info("Template for stack: " + stackName);
		this.logger.info(resultJSON);
		// create or update the template
		this.cloudFormationClientWrapper.createOrUpdateStack(new CreateOrUpdateStackRequest().withStackName(stackName)
				.withTemplateBody(resultJSON).withParameters(parameter));

	}

	/**
	 * The color's private subnets that are in an availability zone able to host the ID generator's DB
	 * instance class.
	 */
	List<String> getDatabaseSubnets(String stack, String color) {
		String privateSubnets = cloudFormationClientWrapper.getOutput(
				Constants.createVpcPrivateSubnetsStackName(stack, color),
				Constants.VPC_PRIVATE_SUBNETS_STACK_PRIVATE_SUBNETS_OUPUT_KEY);
		List<String> subnetIds = Arrays.stream(privateSubnets.split(",")).map(String::trim)
				.collect(Collectors.toList());
		return rdsClientWrapper.getAvailableSubnetsForDBInstanceClasses(Constants.RDS_ENGINE,
				Constants.RDS_ENGINE_VERSION, List.of(ID_GENERATOR_DB_INSTANCE_CLASS), subnetIds,
				Constants.RDS_MINIMUM_SUBNET_COUNT);
	}

}
