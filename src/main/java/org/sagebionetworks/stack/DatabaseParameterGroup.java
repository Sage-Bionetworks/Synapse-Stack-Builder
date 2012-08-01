package org.sagebionetworks.stack;

import static org.sagebionetworks.stack.Constants.ERROR_CODE_DB_PARAMETER_GROUP_NOT_FOUND;
import static org.sagebionetworks.stack.Constants.MYSQL_5_5_DB_PARAMETER_GROUP_FAMILY;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.ApplyMethod;
import com.amazonaws.services.rds.model.CreateDBParameterGroupRequest;
import com.amazonaws.services.rds.model.DBParameterGroup;
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsRequest;
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsResult;
import com.amazonaws.services.rds.model.DescribeDBParametersRequest;
import com.amazonaws.services.rds.model.DescribeDBParametersResult;
import com.amazonaws.services.rds.model.ModifyDBParameterGroupRequest;
import com.amazonaws.services.rds.model.Parameter;

/**
 * Creates and maintains the MySQL database parameter group used by all database instances
 * 
 * @author John
 *
 */
public class DatabaseParameterGroup {

	private static Logger log = Logger.getLogger(DatabaseParameterGroup.class);
	
	AmazonRDSClient client;
	InputConfiguration config;

	/**
	 * IoC constructor.
	 * 
	 * @param client
	 * @param config
	 */
	public DatabaseParameterGroup(AmazonRDSClient client, InputConfiguration config){
		if(client == null) throw new IllegalArgumentException("AmazonRDSClient cannot be null");
		if(config == null) throw new IllegalArgumentException("Config cannot be null");
		this.client = client;
		this.config = config;
	}
	/**
	 * Setup the DB parameter group with all of the values we want to use.
	 * @param client
	 * @param stack
	 * @return
	 */
	public DBParameterGroup setupDBParameterGroup(){
		// First get the group
		DBParameterGroup paramGroup = createOrGetDatabaseParameterGroup();
		// Now make sure it has the parameters we want
		Map<String, Parameter> map = getAllDBGroupParams(paramGroup.getDBParameterGroupName());
		// Turn on the slow query log.
		setValueIfNeeded(paramGroup.getDBParameterGroupName(), map, Constants.DB_PARAM_KEY_SLOW_QUERY_LOG, "1");
		// Set the slow query time (how long a query must be to get recored in the slow query log) in seconds..
		setValueIfNeeded(paramGroup.getDBParameterGroupName(), map, Constants.DB_PARAM_KEY_LONG_QUERY_TIME, "1");
		// Set any other values...
		
		return paramGroup;
	}
	
	/**
	 * Set the value of the given parameter if it does not already have the passed value.
	 * 
	 * @param client
	 * @param map
	 * @param key
	 * @param value
	 * @return true if changed.
	 */
	boolean setValueIfNeeded(String paramGroupName, Map<String, Parameter> map, String key, String value){
		// Check the slow query log value
		Parameter param = map.get(key);
		if(param == null) throw new IllegalStateException("Cannot find the expected DB parameter: "+key);
		// Do we need to change it?
		if(!value.equals(param.getParameterValue())){
			// We need to change the value.
			ModifyDBParameterGroupRequest modifyRequest = new ModifyDBParameterGroupRequest();
			modifyRequest.setDBParameterGroupName(paramGroupName);
			List<Parameter> list = new LinkedList<Parameter>();
			list.add(param);
			param.setParameterValue(value);
			param.setApplyMethod(ApplyMethod.Immediate);
			modifyRequest.setParameters(list);
			client.modifyDBParameterGroup(modifyRequest);
			return true;
		}
		return false;
	}
	
	/**
	 * Create or get the DBParameter group used by this stack.
	 * @param stack
	 */
	public DBParameterGroup createOrGetDatabaseParameterGroup(){
		// The group name
		String groupName = config.getDatabaseParameterGroupName();
		// First query for the group
		try{
			// Query for this group
			DescribeDBParameterGroupsResult result = client.describeDBParameterGroups(new DescribeDBParameterGroupsRequest().withDBParameterGroupName(groupName));
			if(result.getDBParameterGroups() == null|| result.getDBParameterGroups().size() != 1) throw new IllegalStateException("Expected one and only one DB parameter group with name: "+groupName);
			log.info("DB parameter group: '"+groupName+"' already exists");
			return result.getDBParameterGroups().get(0);
		}catch(AmazonServiceException e){
			if(ERROR_CODE_DB_PARAMETER_GROUP_NOT_FOUND.equals(e.getErrorCode())){
				log.info("Creating DB parameter group: '"+groupName+"' for the first time...");
				// We need to create it since it does not exist
				CreateDBParameterGroupRequest request = new CreateDBParameterGroupRequest();
				request.setDBParameterGroupFamily(MYSQL_5_5_DB_PARAMETER_GROUP_FAMILY);
				request.setDBParameterGroupName(groupName);
				request.setDescription(config.getDatabaseParameterGroupDescription());
				return client.createDBParameterGroup(request);
			}else{
				// Any other error gets thrown
				throw e;
			}
		}

	}
	
	/**
	 * The DB group parameters are paged so get all pages.
	 * 
	 * @param client
	 * @param dbGroupName
	 * @return
	 */
	Map<String, Parameter> getAllDBGroupParams(String dbGroupName){
		log.info("Fetching all DB group parameters...");
		List<Parameter> fullParams = new LinkedList<Parameter>();
		DescribeDBParametersResult results = client.describeDBParameters(new DescribeDBParametersRequest().withDBParameterGroupName(dbGroupName));
		fullParams.addAll(results.getParameters());
		String marker = results.getMarker();
		while(marker != null){
			log.info("Fetching next page of DB group parameters. Count: "+fullParams.size());
			results = client.describeDBParameters(new DescribeDBParametersRequest().withDBParameterGroupName(dbGroupName).withMarker(marker));
			fullParams.addAll(results.getParameters());
			marker = results.getMarker();
		}
		log.info("DB parameters count: "+fullParams.size());
		// Put all of the parameters into a map
		Map<String, Parameter> map = new HashMap<String, Parameter>();
		for(Parameter param: fullParams){
			log.info(param.toString());
			map.put(param.getParameterName(), param);
		}
		return map;
	}
}
