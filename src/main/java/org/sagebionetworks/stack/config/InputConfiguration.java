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
import java.util.HashMap;
import java.util.Map;


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
	 * Create the union of the passed properties and all of the configuration properties.
	 * @param props
	 * @return
	 */
	public Properties createUnionOfInputAndConfig(Properties props){
		Properties results = new Properties();
		// Add all of the input properties
		results.putAll(props);
		// Add all of the configuration properties
		results.putAll(this.props);
		return results;
	}
	
	/**
	 * Given input properties with values containing ${..} regular expressions, create
	 * a new properties object with the values replaced from the InputConfiguration.
	 * @param input
	 */
	public Properties createFilteredProperties(Properties input) {
		// First create the union
		Properties union = createUnionOfInputAndConfig(input);
		// Use the filter to replace all regular expressions in the property values
		PropertyFilter.replaceAllRegularExp(union);
		// Build up the filtered properties.
		Properties filterdInput = new Properties();
		for(String key: input.stringPropertyNames()){
			// The value comes from the filtered union.
			String value = union.getProperty(key);
			filterdInput.put(key, value);
		}
		return filterdInput;
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
	public static Properties loadPropertyFile(String fileName){
		log.info("Loading "+fileName);
		InputStream in = InputConfiguration.class.getClassLoader().getResourceAsStream(fileName);
		if(in == null) throw new IllegalArgumentException("Cannot find the required builder properties file on the classpath: "+fileName);
		Properties props = new Properties();
		try {
			props.load(in);
		} catch (IOException e) {
			// convert to runtime.
			throw new RuntimeException(e);
		}
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
	 * The main bucket where all files for this stack reside.
	 */
	public String getMainFileS3BucketName() {
		return validateAndGetProperty("main.file.s3.bucket");
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

	/**
	 * The name of the RDS alert topic. This topic is used to notify by
	 * email, when RDS alarms are triggered.
	 * 
	 * @return
	 */
	public String getRDSAlertTopicName() {
		return validateAndGetProperty("stack.rds.alert.topic.name");
	}

	/**
	 * The RDS alert topic subscription endpoint.
	 * @return
	 */
	public String getRDSAlertSubscriptionEndpoint() {
		return validateAndGetProperty(Constants.KEY_RDS_ALAERT_SUBSCRIPTION_ENDPONT);
	}

	/**
	 * The bucket where all stack instance configuration can be found.
	 * @return
	 */
	public String getStackConfigS3BucketName() {
		return validateAndGetProperty("stack.config.s3.bucket.name");
	}

	/**
	 * The path relative to the bucket of the stack configuration file.
	 * @return
	 */
	public String getStackConfigurationFileS3Path() {
		return validateAndGetProperty("stack.config.property.file.path");
	}

	/**
	 * The full URL of the final stack configuration File.
	 * @return
	 */
	public String getStackConfigurationFileURL() {
		return validateAndGetProperty("stack.config.property.file.url");
	}



	/**
	 * The name of this elastic beanstalk application.
	 * @return
	 */
	public String getElasticBeanstalkApplicationName() {
		return validateAndGetProperty("elastic.beanstalk.application.name");
	}
	
	/**
	 * The URL of the artifact for a given prefix
	 * @return
	 */
	public String getArtifactoryUrl(String prefix) {
		return validateAndGetProperty(prefix+".artifactory.url");
	}

	/**
	 * The version label for a war for a given prefix.
	 * @return
	 */
	public String getVersionLabel(String prefix) {
		return validateAndGetProperty(prefix+".version.label");
	}
	
	/**
	 * Get the version path for a given prefix.
	 * @param prefix
	 * @return
	 */
	public String getVersionPath(String prefix){
		return validateAndGetProperty(prefix+".elastic.beanstalk.application.versions.s3.path");
	}

	/**
	 * The name of the SSL certificate private key.
	 * @return
	 */
	public String getSSlCertificatePrivateKeyName(String prefix) {
		return validateAndGetProperty(prefix + ".ssl.certificate.privateKey.file.name");
	}

	/**
	 * The name of the SSL certificate body file.
	 * @return
	 */
	public String getSSLCertificateBodyKeyName(String prefix) {
		return validateAndGetProperty(prefix + ".ssl.certificate.body.file.name");
	}
	
	/**
	 * The name of the SSL certificate chain file
	 * @return
	 */
	public String getSSLCertificateChainKeyName(String prefix) {
		return validateAndGetProperty(prefix + ".ssl.certificate.chain.file.name");
	}

	/**
	 * The name of the SSL certificate
	 * @return
	 */
	public String getSSLCertificateName(String prefix) {
		return validateAndGetProperty(prefix + ".ssl.certificate.name");
	}
	
	/**
	 * Get the environment name for a given service prefix.
	 * @param prefix
	 * @return
	 */
	public String getEnvironmentName(String prefix) {
		return validateAndGetProperty(prefix+".service.environment.name");
	}
	
	/**
	 * Get the environment CNAME prefix for a given service prefix.
	 * @param prefix
	 * @return
	 */
	public String getEnvironmentCNAMEPrefix(String prefix) {
		return validateAndGetProperty(prefix+".service.environment.cname.prefix");
	}

	/**
	 * The key pair name used by this stack.
	 * @return
	 */
	public String getStackKeyPairName() {
		return validateAndGetProperty("elastic.stack.key.pair.name");
	}
	
	/**
	 * The name of the S3 File that contains this stack's keypair.
	 * 
	 * @return
	 */
	public String getStackKeyPairS3File(){
		return validateAndGetProperty("elastic.stack.key.pair.s3.file.key");
	}

	/**
	 * The name of elastic beanstalk environment template for this instance.
	 * @return
	 */
	public String getElasticBeanstalkTemplateName() {
		return validateAndGetProperty("elastic.beanstalk.environment.template.name");
	}

//	public void setSSLCertificateARN(String prefix, String arn) {
//		props.setProperty(prefix + "." + Constants.KEY_SSL_CERTIFICATE_ARN, arn);
//	}
//	
//	/**
//	 * Get the SSL Certificate ARN
//	 * @return
//	 */
//	public String getSSLCertificateARN(String prefix){
//		return validateAndGetProperty(prefix + "." + Constants.KEY_SSL_CERTIFICATE_ARN);
//	}
//
	/**
	 * Search index domain name.
	 * @return
	 */
	public String getSearchIndexDomainName() {
		return validateAndGetProperty("search.index.domain.name");
	}

	public String getStackSubdomain() {
		return validateAndGetProperty("stack.subdomain");
	}
	
	public String getEnvironmentSubdomainCNAME(String prefix) {
		return validateAndGetProperty(prefix + ".service.environment.subdomain.cname");
	}
	
	public String getPortalBeanstalkNumber() {
		return validateAndGetProperty(Constants.PORTAL_BEANSTALK_NUMBER);
	}
	
	/**
	 * Get the name of the role that grants elasticbeanstalk access to S3 for log rolling.
	 * @return
	 */
	public String getElasticBeanstalkS3RoleName(){
		return validateAndGetProperty("elastic.beanstalk.s3.role.name");
	}

	public String getNumberTableInstances() {
		return validateAndGetProperty(Constants.NUMBER_TABLE_INSTANCES);
	}
	
	public String getStackInstanceTablesDatabaseIdentifierBase() {
		return validateAndGetProperty("stack.table.instance.database.identifier");
	}
	public String getStackInstanceTablesDBSchema() {
		return validateAndGetProperty("stack.table.instance.database.schema");
	}
	public String getStackInstanceTablesDBMasterUser() {
		return validateAndGetProperty("stack.table.instance.database.master.user");
	}
	
	public String getStackTableDBInstanceDatabaseIdentifier(int inst) {
		return validateAndGetProperty("stack.table.instance.database.identifier")+inst;
	}
	
}
