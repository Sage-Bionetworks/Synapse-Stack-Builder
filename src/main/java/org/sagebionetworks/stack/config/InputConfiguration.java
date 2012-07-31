package org.sagebionetworks.stack.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.sagebionetworks.stack.Constants;
import org.sagebionetworks.stack.util.EncryptionUtils;
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
	
	Properties props;
		
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
	String validateAndGetProperty(String key){
		if(key == null) throw new IllegalArgumentException("Property key cannot be null");
		String value = props.getProperty(key);
		if(value == null) throw new IllegalStateException("Cannot find property: "+key);
		if("".equals(value.trim())) throw new IllegalStateException("Propery value is empty for key: "+key);
		return value;
	}
	
	/**
	 * Add properties with plaintext values.  Any property ending with .plaintext will have
	 * its value encrypted and a property added with the same name ending in .encrypted
	 * @param defaultStackProperties
	 */
	public void addPropertiesWithPlaintext(Properties defaultStackProperties) {
		props.putAll(defaultStackProperties);
		// Find any property with a 'plaintext' suffix
		for(Object keyOb: defaultStackProperties.keySet()){
			String key = (String) keyOb;
			if(key.endsWith(Constants.PLAIN_TEXT_SUFFIX)){
				// get the plain text value and encrypt it
				String plainTextValue = defaultStackProperties.getProperty(key);
				String cipherText = EncryptionUtils.encryptString(getEncryptionKey(), plainTextValue);
				// Add this property to the set with the encrypted suffix
				String encryptedKey = key.replaceAll(Constants.PLAIN_TEXT_SUFFIX, Constants.ENCRYPTED_SUFFIX);
				// Add the encrypted property
				props.put(encryptedKey, cipherText);
			}
		}
	}
	
	/**
	 * Is this a production stack? 
	 * @return
	 */
	public boolean isProductionStack() {
		return Constants.PRODUCTION_STACK.equals(getStack().toLowerCase());
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
	public String getEncryptionKey() {
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


	/**
	 * The elastic security group name that all EC2 instances of this stack belong to.
	 * @return
	 */
	public String getElasticSecurityGroupName() {
		return validateAndGetProperty("elastic.security.group.name");
	}

	/**
	 * The elastic security group description.
	 * 
	 * @return
	 */
	public String getElasticSecurityGroupDescription() {
		return validateAndGetProperty("elastic.security.group.description");
	}

	/**
	 * Classless Inter-Domain Routing (CIDR) used to grant SSH access to all machines in the stack.
	 * 
	 * @return
	 */
	public String getCIDRForSSH() {
		return validateAndGetProperty(Constants.KEY_CIDR_FOR_SSH);
	}

	/**
	 * The database parameter group contains all MySQL database parameters.
	 * This parameter group is applied to all MySQL database.
	 * The group name uniquely identifies the group.
	 * @return
	 */
	public String getDatabaseParameterGroupName() {
		return validateAndGetProperty("database.parameter.group.name");
	}

	/**
	 * The database parameter group contains all MySQL database parameters.
	 * This parameter group is applied to all MySQL database.
	 * This is the description of the group.
	 * @return
	 */
	public String getDatabaseParameterGroupDescription() {
		return validateAndGetProperty("database.parameter.group.description");
	}

	/**
	 * The schema name of the ID generator database.
	 * 
	 * @return
	 */
	public String getIdGeneratorDatabaseSchemaName() {
		return validateAndGetProperty("id.gen.database.schema");
	}

	/**
	 * The unique identifier of the ID generator database instances.
	 * 
	 * @return
	 */
	public String getIdGeneratorDatabaseIdentifier() {
		return validateAndGetProperty("id.gen.database.identifier");
	}

	/**
	 * The master username of the ID generator database.
	 * @return
	 */
	public String getIdGeneratorDatabaseMasterUsername() {
		return validateAndGetProperty("id.gen.database.master.user");
	}

	/**
	 * The plain text (non-encrypted) master password of the ID
	 * generator database. 
	 * @return
	 */
	public String getIdGeneratorDatabaseMasterPasswordPlaintext() {
		return validateAndGetProperty(Constants.KEY_DEFAULT_ID_GEN_PASSWORD_PLAIN_TEXT);
	}

	/**
	 * The identifier of the stack database.
	 * 
	 * @return
	 */
	public String getStackInstanceDatabaseIdentifier() {
		return validateAndGetProperty("stack.instance.database.identifier");
	}

	/**
	 * The schema of this stack's MySQL database
	 * @return
	 */
	public String getStackInstanceDatabaseSchema() {
		return validateAndGetProperty("stack.instance.database.schema");
	}

	/**
	 * The master user of this stack's MySQL database.
	 * @return
	 */
	public String getStackInstanceDatabaseMasterUser() {
		return validateAndGetProperty("stack.instance.database.master.user");
	}
	
	/**
	 * The master user of this stack's MySQL database.
	 * @return
	 */
	public String getStackInstanceDatabaseMasterPasswordPlaintext() {
		return validateAndGetProperty(Constants.KEY_DEFAULT_STACK_INSTANCES_DB_PASSWORD_PLAIN_TEXT);
	}

	/**
	 * The database security group name used by the ID generator.
	 * @return
	 */
	public String getIdGeneratorDatabaseSecurityGroupName() {
		return validateAndGetProperty("id.gen.database.security.group.name");
	}

	/**
	 * The database security group description used by the ID generator.
	 * @return
	 */
	public String getIdGeneratorDatabaseSecurityGroupDescription() {
		return validateAndGetProperty("id.gen.database.security.group.description");
	}

	/**
	 * The database security group name used by the Stack's MySQL database.
	 * @return
	 */
	public String getStackDatabaseSecurityGroupName() {
		return validateAndGetProperty("stack.database.security.group.name");
	}

	/**
	 * The database security group description used by the Stack's MySQL database.
	 * @return
	 */
	public String getStackDatabaseSecurityGroupDescription() {
		return validateAndGetProperty("stack.database.security.group.description");
	}

}
