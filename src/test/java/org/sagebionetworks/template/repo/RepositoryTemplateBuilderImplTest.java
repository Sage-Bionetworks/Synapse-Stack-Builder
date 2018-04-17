package org.sagebionetworks.template.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import java.util.Properties;

import static org.sagebionetworks.template.Constants.*;

import org.apache.logging.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.PropertyProvider;
import org.sagebionetworks.template.SystemPropertyProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.vpc.Color;

import com.amazonaws.services.cloudformation.model.Parameter;

@RunWith(MockitoJUnitRunner.class)
public class RepositoryTemplateBuilderImplTest {

	private static final String KEY_STORAGE = "org.sagebionetworks.repo.rds.allocated.storage";
	private static final String KEY_INSTANCE_CLASS = "org.sagebionetworks.repo.rds.instance.class";
	
	@Mock
	CloudFormationClient mockCloudFormationClient;
	@Mock
	PropertyProvider mockPropertyProvider;
	@Mock
	LoggerFactory mockLoggerFactory;
	@Mock
	Logger mockLogger;

	VelocityEngine velocityEngine;
	RepositoryTemplateBuilderImpl builder;

	String stack;
	String instance;
	String vpcSubnetColor;
	String beanstalkNumber;
	
	Properties systemProperties;
	Properties defaultProperties;

	@Before
	public void before() {
		// use a real velocity engine
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();

		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);

		builder = new RepositoryTemplateBuilderImpl(mockCloudFormationClient, velocityEngine, mockPropertyProvider,
				mockLoggerFactory);

		stack = "dev";
		instance = "101";
		vpcSubnetColor = Color.Green.name();
		beanstalkNumber = "2";

		when(mockPropertyProvider.getProperty(PROPERTY_KEY_STACK)).thenReturn(stack);
		when(mockPropertyProvider.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(instance);
		when(mockPropertyProvider.getProperty(PROPERTY_KEY_VPC_SUBNET_COLOR)).thenReturn(vpcSubnetColor);
		when(mockPropertyProvider.getProperty(PROPERTY_KEY_MYSQL_PASSWORD)).thenReturn("somePassword");
		
		systemProperties = new Properties();
		// override the storage size.
		systemProperties.put(KEY_STORAGE, "123");
		defaultProperties = new SystemPropertyProvider().loadPropertiesFromClasspath(Constants.DEFAULT_REPO_PROPERTIES);
		
		when(mockPropertyProvider.getSystemProperties()).thenReturn(systemProperties);
		when(mockPropertyProvider.loadPropertiesFromClasspath(Constants.DEFAULT_REPO_PROPERTIES)).thenReturn(defaultProperties);
	}

	@Test
	public void testBuildAndDeploy() {
		// Set different values for the tables database
		systemProperties.put("org.sagebionetworks.tables.rds.instance.count", "2");
		systemProperties.put("org.sagebionetworks.tables.rds.allocated.storage", "3");
		systemProperties.put("org.sagebionetworks.tables.rds.instance.class", "db.t2.micro");
		
		// call under test
		builder.buildAndDeploy();
		ArgumentCaptor<String> nameCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> bodyCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Parameter[]> parameterCatpure = ArgumentCaptor.forClass(Parameter[].class);
		verify(mockCloudFormationClient).createOrUpdateStack(nameCapture.capture(), bodyCapture.capture(),
				parameterCatpure.capture());
		assertEquals("dev-101-shared-resources", nameCapture.getValue());
		assertNotNull(parameterCatpure.getValue());
		String bodyJSONString = bodyCapture.getValue();
		assertNotNull(bodyJSONString);
		System.out.println(bodyJSONString);
		JSONObject templateJson = new JSONObject(bodyJSONString);
		JSONObject resources = templateJson.getJSONObject("Resources");
		assertNotNull(resources);
		// database group
		validateResouceDatabaseSubnetGroup(resources);
		// database instance
		validateResouceDatabaseInstance(resources);
		// tables database
		validateResouceTablesDatabase(resources);
	}

	public void validateResouceDatabaseSubnetGroup(JSONObject resources) {
		JSONObject subnetGroup = resources.getJSONObject("dev101DBSubnetGroup");
		assertNotNull(subnetGroup);
		JSONObject properties = subnetGroup.getJSONObject("Properties");
		JSONArray subnetArray = properties.getJSONArray("SubnetIds");
		JSONObject subnetOne = subnetArray.getJSONObject(0);
		assertEquals("us-east-1-synapse-dev-vpc-GreenPrivate0Subnet-ID", subnetOne.getString("Fn::ImportValue"));
		JSONObject subnetTwo = subnetArray.getJSONObject(1);
		assertEquals("us-east-1-synapse-dev-vpc-GreenPrivate1Subnet-ID", subnetTwo.getString("Fn::ImportValue"));
	}
	
	/**
	 * Validate for the repo AWS::RDS::DBInstance.
	 * @param resources
	 */
	public void validateResouceDatabaseInstance(JSONObject resources) {
		JSONObject instance = resources.getJSONObject("dev101RepositoryDB");
		assertNotNull(instance);
		JSONObject properties = instance.getJSONObject("Properties");
		assertEquals("123", properties.get("AllocatedStorage"));
		assertEquals("db.t2.small", properties.get("DBInstanceClass"));
		assertEquals(Boolean.FALSE, properties.get("MultiAZ"));
	}
	
	/**
	 * Validate tables for AWS::RDS::DBInstance
	 * @param resources
	 */
	public void validateResouceTablesDatabase(JSONObject resources) {
		// zero
		JSONObject instance = resources.getJSONObject("dev101Table0RepositoryDB");
		assertNotNull(instance);
		JSONObject properties = instance.getJSONObject("Properties");
		assertEquals("3", properties.get("AllocatedStorage"));
		assertEquals("db.t2.micro", properties.get("DBInstanceClass"));
		assertEquals(Boolean.FALSE, properties.get("MultiAZ"));
		assertEquals("dev-101-table-0", properties.get("DBInstanceIdentifier"));
		assertEquals("dev101table0", properties.get("DBName"));
		// one
		instance = resources.getJSONObject("dev101Table1RepositoryDB");
		assertNotNull(instance);
		properties = instance.getJSONObject("Properties");
		assertEquals("3", properties.get("AllocatedStorage"));
		assertEquals("db.t2.micro", properties.get("DBInstanceClass"));
		assertEquals(Boolean.FALSE, properties.get("MultiAZ"));
		assertEquals("dev-101-table-1", properties.get("DBInstanceIdentifier"));
		assertEquals("dev101table1", properties.get("DBName"));
	}

	@Test
	public void testGetParamters() {
		// call under test
		Parameter[] params = builder.createParameters();
		assertNotNull(params);
		assertEquals(1, params.length);
		Parameter param = params[0];
		assertEquals(PARAMETER_MYSQL_PASSWORD, param.getParameterKey());
		assertEquals("somePassword", param.getParameterValue());
	}

	@Test
	public void testcreateSharedResourcesStackName() {
		// call under test
		String name = builder.createSharedResourcesStackName();
		assertEquals("dev-101-shared-resources", name);
	}

	@Test
	public void testCreateVpcExportPrefix() {
		// call under test
		String vpcPrefix = builder.createVpcExportPrefix();
		assertEquals("us-east-1-synapse-dev-vpc", vpcPrefix);
	}

	@Test
	public void testCreateContext() {
		// call under test
		VelocityContext context = builder.createContext();
		assertNotNull(context);
		assertEquals("dev", context.get(STACK));
		assertEquals("101", context.get(INSTANCE));
		assertEquals("Green", context.get(VPC_SUBNET_COLOR));
		assertEquals("dev-101-shared-resources", context.get(SHARED_RESOUCES_STACK_NAME));
		assertEquals("us-east-1-synapse-dev-vpc", context.get(VPC_EXPORT_PREFIX));
		
		Properties props = (Properties) context.get(PROPS);
		assertNotNull(props);
		assertEquals("123", props.get(KEY_STORAGE));
		// should not match the default
		assertFalse(props.get(KEY_STORAGE).equals(defaultProperties.get(KEY_STORAGE)));
		// should match the default since it is not overridden.
		assertTrue(props.get(KEY_INSTANCE_CLASS).equals(defaultProperties.get(KEY_INSTANCE_CLASS)));
	}
	
	@Test
	public void testTableDatabaseSuffixes() {
		String[] results = builder.tableDatabaseSuffixes(3);
		assertNotNull(results);
		assertEquals("0",results[0]);
		assertEquals("1",results[1]);
		assertEquals("2",results[2]);
 	}
}
