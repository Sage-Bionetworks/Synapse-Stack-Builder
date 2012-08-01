package org.sagebionetworks.stack;

import java.io.IOException;
import java.util.Properties;

import org.sagebionetworks.stack.config.InputConfiguration;

/**
 * Helper used by tests for setup.
 * 
 * @author John
 *
 */
public class InputConfigHelper {
	
	/**
	 * Create a test Config.
	 * @return
	 * @throws IOException
	 */
	public static InputConfiguration createTestConfig(String stack) throws IOException{
		Properties inputProperties = new Properties();
		inputProperties.put(Constants.AWS_ACCESS_KEY, "AWS access key value");
		inputProperties.put(Constants.AWS_SECRET_KEY, "AWS secrite key value");
		inputProperties.put(Constants.STACK_ENCRYPTION_KEY, "Encryption key that is long enough");
		inputProperties.put(Constants.STACK, stack);
		inputProperties.put(Constants.INSTANCE, "A");
		InputConfiguration config = new InputConfiguration(inputProperties);
		Properties defaults = new Properties();
		defaults.put(Constants.KEY_DEFAULT_ID_GEN_PASSWORD_PLAIN_TEXT, "id gen password");
		defaults.put(Constants.KEY_DEFAULT_STACK_INSTANCES_DB_PASSWORD_PLAIN_TEXT, "stack db password");
		defaults.put(Constants.KEY_CIDR_FOR_SSH, "255.255.255/1");
		defaults.put(Constants.KEY_RDS_ALAERT_SUBSCRIPTION_ENDPONT, "dev@sagebaser.org");
		config.addPropertiesWithPlaintext(defaults);
		return config;
	}

}
