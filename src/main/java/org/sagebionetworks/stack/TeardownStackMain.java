package org.sagebionetworks.stack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.sagebionetworks.stack.alarms.RdsAlarmSetup;
import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.factory.AmazonClientFactory;
import org.sagebionetworks.stack.factory.AmazonClientFactoryImpl;
import org.sagebionetworks.stack.notifications.StackInstanceNotificationSetup;

/**
 *
 * @author xavier
 */
public class TeardownStackMain {

	private static Logger log = Logger.getLogger(BuildStackMain.class.getName());

	/**
	 * This is the main script that builds the configuration.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			// Log the args.
			logArgs(args);
			// The path to the configuration property file must be provided
			if (args == null || args.length != 1) {
				throw new IllegalArgumentException("The first argument must be the path configuration property file to be used to build this stack.");
			}
			String pathConfig = args[0];
			// Load the properites file
			Properties input = loadPropertiesFromPath(pathConfig);

			AmazonClientFactory factory = new AmazonClientFactoryImpl();

			GeneratedResources resources = describeStack(input, factory);
			
			teardownStack(input, factory, resources);

		} catch (Throwable e) {
			log.error("Terminating: ", e);
		} finally {
			log.info("Terminating stack builder\n\n\n");
		}
	}

	public static GeneratedResources describeStack(Properties inputProps, AmazonClientFactory factory) throws IOException {
		// First load the configuration properties.
		InputConfiguration config = new InputConfiguration(inputProps);
		// Set the credentials
		factory.setCredentials(config.getAWSCredentials());
		// Load the default properties used for this stack
		Properties defaultStackProperties = new StackDefaults(factory.createS3Client(), config).loadStackDefaultsFromS3();
		// Add the default properties to the config
		// Note: This is where all encrypted properties are also added.
		config.addPropertiesWithPlaintext(defaultStackProperties);

		GeneratedResources resources = new GeneratedResources();

		// Since the search index can take time to setup, we buid it first.
		new SearchIndexSetup(factory, config, resources).describeResources();

		// The first step is to setup the stack security
		new EC2SecuritySetup(factory, config, resources).describeResources();

		// Setup the notification topic.
		new StackInstanceNotificationSetup(factory, config, resources).describeResources();

		// Setup the Database Parameter group
		new DatabaseParameterGroup(factory, config, resources).describeResources();

		// Setup all of the database security groups
		new DatabaseSecuritySetup(factory, config, resources).describeResources();

		// We are ready to create the database instances
		new MySqlDatabaseSetup(factory, config, resources).describeResources();

		// Add all of the the alarms
		new RdsAlarmSetup(factory, config, resources).describeResources();

//		// Create the configuration file and upload it S3
//		new StackConfigurationSetup(factory, config, resources).describeResources();

		// Process the artifacts
		new ArtifactProcessing(new DefaultHttpClient(), factory, config, resources).describeResources();
		
		// Setup the SSL certificates
		new SSLSetup(factory, config, resources).describeResources("generic");

		// Setup all environments
		new ElasticBeanstalkSetup(factory, config, resources).describeResources();

		return resources;
	}

	public static void teardownStack(Properties inputProps, AmazonClientFactory factory, GeneratedResources resources) throws IOException {
		// First load the configuration properties.
		InputConfiguration config = new InputConfiguration(inputProps);
		// Set the credentials
		factory.setCredentials(config.getAWSCredentials());
		// Load the default properties used for this stack
		Properties defaultStackProperties = new StackDefaults(factory.createS3Client(), config).loadStackDefaultsFromS3();
		// Add the default properties to the config
		// Note: This is where all encrypted properties are also added.
		config.addPropertiesWithPlaintext(defaultStackProperties);

		// Terminate environments
		new ElasticBeanstalkSetup(factory, config, resources).teardownResources();

		// Don't deal with SSL cert now

		// Don't deal with artifacts now

		// Don't deal with config file now

		// Don't deal with alarms now

		// Delete stack database instance
		new MySqlDatabaseSetup(factory, config, resources).teardownResources();

		// Don't deal with DB security group now

		// Don't deal DB parameter group now

		// Don't deal with notification topic now

		// Don't deal with EC2 security group now

	}

	/**
	 * Load the properties from the passed path.
	 *
	 * @param pathConfig
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static Properties loadPropertiesFromPath(String pathConfig)
			throws FileNotFoundException, IOException {
		// Load the config file
		File configFile = new File(pathConfig);
		if (!configFile.exists()) {
			throw new IllegalArgumentException("Passed configuration file does not exist: " + configFile.getAbsolutePath());
		}
		FileInputStream in = new FileInputStream(configFile);
		try {
			Properties props = new Properties();
			props.load(in);
			return props;
		} finally {
			in.close();
		}
	}

	/**
	 * Write the arguments to the log.
	 *
	 * @param args
	 */
	private static void logArgs(String[] args) {
		log.info("Starting Stack Builder...");
		if (args != null) {
			log.info("args[] length=" + args.length);
			for (String arg : args) {
				log.info(arg);
			}
		}
	}
}
