package org.sagebionetworks.stack;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;

import org.sagebionetworks.stack.factory.AmazonClientFactoryImpl;

/**
 *
 * @author xschildw
 */
public class StackActivator {
	private static Logger log = Logger.getLogger(BuildStackMain.class.getName());
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String stackInstance, instanceRole;
		try {
			String configFilePath;
			Properties inputProps = null;

			log.info("Starting StackActivator...");

			// TODO: Better args checking
			if ((args != null) && (args.length == 3)) {
				stackInstance = args[0];
				instanceRole = args[1];
				configFilePath = args[2];

				if (configFilePath != null) {
					inputProps = loadPropertiesFromPath(configFilePath);
				} else {
					inputProps = System.getProperties();
				}
				
				if (! (("prod".equals(instanceRole)) || ("staging".equals(instanceRole)))) {
					showUsage();
					throw new IllegalArgumentException("Valid values for instanceRole are 'prod' or 'staging'.");
				}
				
				Activator activator = new Activator(new AmazonClientFactoryImpl(), inputProps, stackInstance, instanceRole);
				activator.activateStack();
			} else {
				showUsage();
				throw new IllegalArgumentException("Wrong number of arguments!");
			}

			log.info("Exiting StackActivator...");
		} catch (Throwable e) {
			log.error("Terminating: ",e);
			e.printStackTrace();
		}finally{
			log.info("Terminating StackActivator\n\n\n");
			System.exit(0);
		}

	}
	
	private static Properties loadPropertiesFromPath(String pathConfig)
			throws FileNotFoundException, IOException {
		// Load the config file
		File configFile = new File(pathConfig);
		if(!configFile.exists()) throw new IllegalArgumentException("Passed configuration file does not exist: "+configFile.getAbsolutePath());
		FileInputStream in =  new FileInputStream(configFile);
		try{
			Properties props = new Properties();
			props.load(in);
			return props;
		}finally{
			in.close();
		}
	}
	
	private static void showUsage() {
		System.out.println("Usage:\n\tstackActivator <stackInstance> <instanceRole> <pathToConfigFile\n");
	}
}
