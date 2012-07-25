package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.stack.Constants.DB_PARAM_GROUP_DESC_TEMPALTE;
import static org.sagebionetworks.stack.Constants.DB_PARAM_GROUP_NAME_TEMPLATE;
import static org.sagebionetworks.stack.Constants.ERROR_CODE_DB_PARAMETER_GROUP_NOT_FOUND;
import static org.sagebionetworks.stack.Constants.MYSQL_5_5_DB_PARAMETER_GROUP_FAMILY;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.stack.DatabaseParameterGroup;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.CreateDBParameterGroupRequest;
import com.amazonaws.services.rds.model.DBParameterGroup;
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsRequest;
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsResult;

public class DatabaseParameterGroupTest {
	
	AmazonRDSClient mockClient = null;
	
	@Before
	public void before(){
		mockClient = Mockito.mock(AmazonRDSClient.class);
	}
	
	@Test
	public void testCreateOrGetDatabaseParameterExists(){
		String stack = "stack";
		DBParameterGroup group = new DBParameterGroup().withDBParameterGroupName("some group name");
		DescribeDBParameterGroupsResult result = new DescribeDBParameterGroupsResult();
		result.setDBParameterGroups(new LinkedList<DBParameterGroup>());
		result.getDBParameterGroups().add(group);
		when(mockClient.describeDBParameterGroups(any(DescribeDBParameterGroupsRequest.class))).thenReturn(result);
		// simulate the group already exists
		DBParameterGroup paramGroup = DatabaseParameterGroup.createOrGetDatabaseParameterGroup(mockClient, stack);
		assertEquals(group, paramGroup);
	}

	@Test
	public void testCreateOrGetDatabaseParameterDoesNotExist(){
		String stack = "stack";
		// this param group should get created.
		DBParameterGroup expected = new DBParameterGroup();
		expected.setDBParameterGroupName(String.format(DB_PARAM_GROUP_NAME_TEMPLATE, stack));
		expected.setDescription(String.format(DB_PARAM_GROUP_DESC_TEMPALTE, stack));
		expected.setDBParameterGroupFamily(MYSQL_5_5_DB_PARAMETER_GROUP_FAMILY);
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
		DBParameterGroup paramGroup = DatabaseParameterGroup.createOrGetDatabaseParameterGroup(mockClient, stack);
		assertEquals(expected, paramGroup);
		verify(mockClient).createDBParameterGroup(request);
	}
	
	@Test (expected=AmazonServiceException.class)
	public void testUnknownError(){
		String stack = "stack";
		// Any unknown error should be re-thrown.
		AmazonServiceException exception = new AmazonServiceException("Not found");
		exception.setErrorCode("Some unknown error");
		when(mockClient.describeDBParameterGroups(any(DescribeDBParameterGroupsRequest.class))).thenThrow(exception);
		// simulate unknonw error
		DatabaseParameterGroup.createOrGetDatabaseParameterGroup(mockClient, stack);
	}
}
