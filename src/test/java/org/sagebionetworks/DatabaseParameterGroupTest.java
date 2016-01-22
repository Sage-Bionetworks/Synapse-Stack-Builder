package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.stack.Constants.ERROR_CODE_DB_PARAMETER_GROUP_NOT_FOUND;
import static org.sagebionetworks.stack.Constants.MYSQL_5_6_DB_PARAMETER_GROUP_FAMILY;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.stack.DatabaseParameterGroup;
import org.sagebionetworks.stack.TestHelper;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.CreateDBParameterGroupRequest;
import com.amazonaws.services.rds.model.DBParameterGroup;
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsRequest;
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsResult;
import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.GeneratedResources;

public class DatabaseParameterGroupTest {
	
	Properties inputProperties;
	InputConfiguration config;
	GeneratedResources resources;
	AmazonRDSClient mockClient = null;
	MockAmazonClientFactory factory = new MockAmazonClientFactory();
	DatabaseParameterGroup databaseParamGroup;
	
	@Before
	public void before() throws IOException{
		
		mockClient = factory.createRDSClient();
		inputProperties = TestHelper.createInputProperties("dev");
		config = new InputConfiguration(inputProperties);
		resources = new GeneratedResources();
		// Inject the dependencies.
		databaseParamGroup = new DatabaseParameterGroup(factory, config, resources);
	}
	
	@Test
	public void testCreateOrGetDatabaseParameterExists(){
		DBParameterGroup group = new DBParameterGroup().withDBParameterGroupName("some group name");
		DescribeDBParameterGroupsResult result = new DescribeDBParameterGroupsResult();
		result.setDBParameterGroups(new LinkedList<DBParameterGroup>());
		result.getDBParameterGroups().add(group);
		when(mockClient.describeDBParameterGroups(any(DescribeDBParameterGroupsRequest.class))).thenReturn(result);
		// simulate the group already exists
		DBParameterGroup paramGroup = databaseParamGroup.createOrGetDatabaseParameterGroup();
		assertEquals(group, paramGroup);
	}

	@Test
	public void testCreateOrGetDatabaseParameterDoesNotExist(){
		String stack = "stack";
		// this param group should get created.
		DBParameterGroup expected = new DBParameterGroup();
		expected.setDBParameterGroupName(config.getDatabaseParameterGroupName());
		expected.setDescription(config.getDatabaseParameterGroupDescription());
		expected.setDBParameterGroupFamily(MYSQL_5_6_DB_PARAMETER_GROUP_FAMILY);
		CreateDBParameterGroupRequest request = new CreateDBParameterGroupRequest();
		request.setDBParameterGroupFamily(expected.getDBParameterGroupFamily());
		request.setDBParameterGroupName(expected.getDBParameterGroupName());
		request.setDescription(expected.getDescription());
		// Throwing an exception with this error code indicates that the group does not exist.
		AmazonServiceException exception = new AmazonServiceException("Not found");
		exception.setErrorCode(ERROR_CODE_DB_PARAMETER_GROUP_NOT_FOUND);
		when(mockClient.describeDBParameterGroups(any(DescribeDBParameterGroupsRequest.class))).thenThrow(exception);
		when(mockClient.createDBParameterGroup(request)).thenReturn(expected);
		// simulate the group already exists
		DBParameterGroup paramGroup = databaseParamGroup.createOrGetDatabaseParameterGroup();
		assertEquals(expected, paramGroup);
		verify(mockClient).createDBParameterGroup(request);
	}
	
	@Test (expected=AmazonServiceException.class)
	public void testUnknownError(){
		// Any unknown error should be re-thrown.
		AmazonServiceException exception = new AmazonServiceException("Not found");
		exception.setErrorCode("Some unknown error");
		when(mockClient.describeDBParameterGroups(any(DescribeDBParameterGroupsRequest.class))).thenThrow(exception);
		// simulate unknown error
		databaseParamGroup.createOrGetDatabaseParameterGroup();
	}
}
