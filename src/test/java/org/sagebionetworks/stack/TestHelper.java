package org.sagebionetworks.stack;

import java.io.IOException;
import java.util.Properties;

import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.Endpoint;
import static org.sagebionetworks.stack.Constants.*;

/**
 * Helper used by tests for setup.
 * 
 * @author John
 *
 */
public class TestHelper {


	/**
	 * Create a test Config.
	 * @return
	 * @throws IOException
	 */
	public static InputConfiguration createTestConfig(String stack) throws IOException{
		Properties inputProperties = createInputProperties(stack);
		InputConfiguration config = new InputConfiguration(inputProperties);
		Properties defaults = createDefaultProperties();
		config.addPropertiesWithPlaintext(defaults);
		return config;
	}

	/**
	 * @return
	 */
	public static Properties createDefaultProperties() {
		Properties defaults = new Properties();
		defaults.put(Constants.KEY_DEFAULT_ID_GEN_PASSWORD_PLAIN_TEXT, "id gen password");
		defaults.put(Constants.KEY_DEFAULT_STACK_INSTANCES_DB_PASSWORD_PLAIN_TEXT, "stack db password");
		defaults.put(Constants.KEY_CIDR_FOR_SSH, "255.255.255/1");
		defaults.put(Constants.KEY_RDS_ALAERT_SUBSCRIPTION_ENDPONT, "dev@sagebaser.org");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_CROWD_APPLICATION_KEY_PLAINTEXT, "crowd-app-key");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_MAIL_PW_PLAINTEXT, "mail password");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_BCC_GOOGLEAPPS_OAUTH_CONSUMER_SECRET_PLAINTEX, "google consumer oath key");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_BCC_GOOGLEAPPS_OAUTH_ACCESS_TOKEN_PLAINTEXT, "google access token");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_BCC_GOOGLEAPPS_OAUTH_ACCESS_TOKEN_SECRET_PLAINTEXT, "google access token secret");
		return defaults;
	}

	/**
	 * Create test input properties.
	 * @param stack
	 * @return
	 */
	public static Properties createInputProperties(String stack) {
		Properties inputProperties = new Properties();
		inputProperties.put(Constants.AWS_ACCESS_KEY, "AWS access key value");
		inputProperties.put(Constants.AWS_SECRET_KEY, "AWS secrite key value");
		inputProperties.put(Constants.STACK_ENCRYPTION_KEY, "Encryption key that is long enough");
		inputProperties.put(Constants.STACK, stack);
		inputProperties.put(Constants.INSTANCE, "A");
		inputProperties.put(Constants.PORTAL_VERSION, "2.4.8");
		inputProperties.put(Constants.AUTHENTICATION_VERSION, "1.2.3");
		inputProperties.put(Constants.REPOSITORY_VERSION, "7.8.9");
		return inputProperties;
	}
	
	/**
	 * Create GeneratedResources that can be used for a test.
	 * 
	 * @param config
	 * @return
	 */
	public static GeneratedResources createTestResources(InputConfiguration config){
		GeneratedResources resoruces  = new GeneratedResources();
		resoruces.setIdGeneratorDatabase(new DBInstance().withDBInstanceIdentifier(config.getIdGeneratorDatabaseIdentifier()).withEndpoint(new Endpoint().withAddress("id-gen-db.someplace.com")));
		resoruces.setStackInstancesDatabase(new DBInstance().withDBInstanceIdentifier(config.getStackInstanceDatabaseIdentifier()).withEndpoint(new Endpoint().withAddress("stack-instance-db.someplace.com")));
		return resoruces;
	}

}
