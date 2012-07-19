package org.sagebionetworks.stack;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;


/**
 * Input Configuration data used to build a stack.
 * 
 * @author John
 *
 */
public class InputConfiguration {
	
	private static Logger log = Logger.getLogger(InputConfiguration.class.getName());
	
	public static final String REQUIRED_BUILDER_PROPERTIES_NAME = "required-builder.properties";
	
	private Properties props;
		
	/**
	 * Create the Configuration object from properties
	 * @param configProps
	 * @throws IOException
	 */
	public InputConfiguration(Properties configProps) throws IOException{
		// Init with these properties
		init(configProps);
	}

	private void init(Properties configProps) throws IOException {
		props = configProps;
		// Validate that all of the required properties are there.
		Properties required = loadRequired();
		// Validate all of the required properties are there.
		validateProperties(required, props);
	}
	
	/**
	 * Get the required properties file.
	 * @return
	 * @throws IOException
	 */
	public static Properties loadRequired() throws IOException{
		log.info("Loading "+REQUIRED_BUILDER_PROPERTIES_NAME);
		InputStream in = InputConfiguration.class.getClassLoader().getResourceAsStream(REQUIRED_BUILDER_PROPERTIES_NAME);
		if(in == null) throw new IllegalArgumentException("Cannot find the required builder properties file on the classpath: "+REQUIRED_BUILDER_PROPERTIES_NAME);
		Properties props = new Properties();
		props.load(in);
		return props;
	}
	
	/**
	 * Validate that all of the required property are present.
	 * @param required
	 * @param loaded
	 */
	public static void validateProperties(Properties required, Properties loaded){
		if(required == null) throw new IllegalArgumentException("The required properties cannot be null");
		if(loaded == null) throw new IllegalArgumentException("The loaded properties cannot be null");
		log.info("Validating properties...");
		for(Object keyOb: required.keySet()){
			String key = (String) keyOb;
			String loadedValue = loaded.getProperty(key);
			log.info(key+"="+loadedValue);
			if(loadedValue == null || loadedValue.length() < 1) throw new IllegalArgumentException("Missing required configuration property, key: "+key);
		}
	}
	
	/**
	 * The Amazon Web Services access key (ID)
	 * @return
	 */
	public String getAWSAccessKey(){
		return props.getProperty(Constants.AWS_ACCESS_KEY);
	}
	
	/**
	 * The Amazon Web Services secret key (password)
	 * @return
	 */
	public String getAWSSecretKey(){
		return props.getProperty(Constants.AWS_SECRET_KEY);
	}
	
	/**
	 * These credentials are used for all AWS communications
	 * @return
	 */
	public AWSCredentials getAWSCredentials(){
		return new BasicAWSCredentials(getAWSAccessKey(), getAWSSecretKey());
	}

	/**
	 * The encryption key used to encrypt all passwords
	 * @return
	 */
	public Object getEncryptionKey() {
		return props.getProperty(Constants.STACK_ENCRYPTION_KEY);
	}

	/**
	 * The name of this stack (prod or dev)
	 * 
	 * @return
	 */
	public String getStack() {
		return props.getProperty(Constants.STACK);
	}

	/**
	 * The name of this stack instance. The unique id of this instance of the stack.  For example, instance=B would be used to create prodB.
	 * For a dev stack this should be your last name.  For example, instance=hill would be used to create devhill
	 * @return
	 */
	public Object getStackInstance() {
		return props.getProperty(Constants.INSTANCE);
	}

}
