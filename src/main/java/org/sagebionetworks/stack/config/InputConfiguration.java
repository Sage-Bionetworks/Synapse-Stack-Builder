package org.sagebionetworks.stack.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.sagebionetworks.stack.Constants;
import org.sagebionetworks.stack.util.PropertyFilter;

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
	public static final String STACK_NAMES_PROPERTIES_FILE = "stack-instance-names.properties";
	
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
		
		// Load the stack names
		Properties stackNames = loadStackNames();
		// Add all of the stack names to the properties file
		props.putAll(stackNames);
		// Use the filter to replace all regular expressions in the property values
		PropertyFilter.replaceAllRegularExp(props);
		log.debug(props);
	}
	
	/**
	 * Get the required properties file.
	 * @return
	 * @throws IOException
	 */
	static Properties loadRequired() throws IOException{
		return loadPropertyFile(REQUIRED_BUILDER_PROPERTIES_NAME);
	}
	
	/**
	 * Get the required properties file.
	 * @return
	 * @throws IOException
	 */
	static Properties loadStackNames() throws IOException{
		return loadPropertyFile(STACK_NAMES_PROPERTIES_FILE);
	}
	
	/**
	 * Load a property file from name.
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static Properties loadPropertyFile(String fileName) throws IOException{
		log.info("Loading "+fileName);
		InputStream in = InputConfiguration.class.getClassLoader().getResourceAsStream(fileName);
		if(in == null) throw new IllegalArgumentException("Cannot find the required builder properties file on the classpath: "+fileName);
		Properties props = new Properties();
		props.load(in);
		return props;
	}
	
	/**
	 * Validate that all of the required property are present.
	 * @param required
	 * @param loaded
	 */
	static void validateProperties(Properties required, Properties loaded){
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
	 * Validate that the requested property exists and is not null
	 * @param key
	 * @return
	 */
	private String validateAndGetProperty(String key){
		if(key == null) throw new IllegalArgumentException("Property key cannot be null");
		String value = props.getProperty(key);
		if(value == null) throw new IllegalStateException("Cannot find property: "+key);
		if("".equals(value.trim())) throw new IllegalStateException("Propery value is empty for key: "+key);
		return value;
	}
	
	public void addDefaultStackProperties(Properties defaultStackProperties) {
		props.putAll(defaultStackProperties);
	}
	
	/**
	 * The Amazon Web Services access key (ID)
	 * @return
	 */
	public String getAWSAccessKey(){
		return validateAndGetProperty(Constants.AWS_ACCESS_KEY);
	}
	
	/**
	 * The Amazon Web Services secret key (password)
	 * @return
	 */
	public String getAWSSecretKey(){
		return validateAndGetProperty(Constants.AWS_SECRET_KEY);
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
		return validateAndGetProperty(Constants.STACK_ENCRYPTION_KEY);
	}

	/**
	 * The name of this stack (prod or dev)
	 * 
	 * @return
	 */
	public String getStack() {
		return validateAndGetProperty(Constants.STACK);
	}

	/**
	 * The name of this stack instance. The unique id of this instance of the stack.  For example, instance=B would be used to create prodB.
	 * For a dev stack this should be your last name.  For example, instance=hill would be used to create devhill
	 * @return
	 */
	public String getStackInstance() {
		return validateAndGetProperty(Constants.INSTANCE);
	}

	/**
	 * The name of the default S3 bucket.
	 * @return
	 */
	public String getDefaultS3BucketName() {
		return validateAndGetProperty("default.stack.s3.bucket");
	}
	
	/**
	 * The file name of the default properties file in S3.
	 * @return
	 */
	public String getDefaultPropertiesFileName() {
		return validateAndGetProperty("default.stack.properties.name");
	}


	public String getElasticSecurityGroupName() {
		return validateAndGetProperty("elastic.security.group.name");
	}

	public String getElasticSecurityGroupDescription() {
		return validateAndGetProperty("elastic.security.group.description");
	}

	public String getCIDRForSSH() {
		return validateAndGetProperty(Constants.KEY_CIDR_FOR_SSH);
	}

	public String getDatabaseParameterGroupName() {
		return validateAndGetProperty("database.parameter.group.name");
	}

	public String getDatabaseParameterGroupDescription() {
		return validateAndGetProperty("database.parameter.group.description");
	}

	public String getIdGeneratorDatabaseSchemaName() {
		return validateAndGetProperty("id.gen.database.schema");
	}

	public String getIdGeneratorDatabaseIdentifier() {
		return validateAndGetProperty("id.gen.database.identifier");
	}

	public String getIdGeneratorDatabaseUsername() {
		return validateAndGetProperty("id.gen.database.user");
	}

	public String getIdGeneratorDatabasePasswordPlaintext() {
		return validateAndGetProperty("id.gen.database.user");
	}


}
