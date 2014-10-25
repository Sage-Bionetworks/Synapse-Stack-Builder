package org.sagebionetworks.stack;

import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.GetHostedZoneRequest;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;

import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.factory.AmazonClientFactory;
import org.sagebionetworks.stack.factory.AmazonClientFactoryImpl;

/**
 *
 * @author xschildw
 */
public class StackActivator {
	private static Logger log = Logger.getLogger(StackActivator.class.getName());
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String stackInstance, instanceRole;
		try {
			String pathConfig;
			Properties inputProps = null;

			log.info("Staring StackActivator...");

			// TODO: Better args checking
			if ((args != null) && (args.length == 3)) {
				stackInstance = args[0];
				instanceRole = args[1];
				pathConfig = args[2];

				if (pathConfig != null) {
					inputProps = loadPropertiesFromPath(pathConfig);
				} else {
					inputProps = System.getProperties();
				}
				
				if (! (("prod".equals(instanceRole)) || ("staging".equals(instanceRole)))) {
					showUsage();
					throw new IllegalArgumentException("Valid values for instanceRole are 'prod' or 'staging'.");
				}
				InputConfiguration config = new InputConfiguration(inputProps);

				Activator activator = new Activator(new AmazonClientFactoryImpl(), config, stackInstance, instanceRole);
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
