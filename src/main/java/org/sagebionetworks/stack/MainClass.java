package org.sagebionetworks.stack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * The main class to start the stack builder
 * @author John
 *
 */
public class MainClass {

	private static Logger log = Logger.getLogger(MainClass.class.getName());
	/**
	 * This is the main script that builds the configuration.
	 * @param args
	 */
	public static void main(String[] args) {
		try{
			// Log the args.
			logArgs(args);
			// The path to the configuration property file must be provided
			if(args == null || args.length != 1) throw new IllegalArgumentException("The first argument must be the path configuration property file to be used to build this stack.");
			String pathConfig = args[0];
			// Load the configuration
			Configuration config = loadConfiguration(pathConfig);
			
			// Create or setup the Id generator database as needed.
			DatabaseInfo idGeneratorDBInfo = IdGeneratorSetup.createIdGeneratorDatabase(config);

		}catch(Throwable e){
			log.error("Terminating: ",e);
		}finally{
			log.info("Terminating stack builder\n\n\n");
		}
	}
	
	/**
	 * Load the configuration from the properties file
	 * 
	 * @param pathConfig
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static Configuration loadConfiguration(String pathConfig)
			throws FileNotFoundException, IOException {
		// Load the config file
		File configFile = new File(pathConfig);
		if(!configFile.exists()) throw new IllegalArgumentException("Passed configuration file does not exist: "+configFile.getAbsolutePath());
		FileInputStream in =  new FileInputStream(configFile);
		try{
			Properties props = new Properties();
			props.load(in);
			return new Configuration(props);
		}finally{
			in.close();
		}
	}
	
	/**
	 * Write the arguments to the log.
	 * @param args
	 */
	private static void logArgs(String[] args) {
		log.info("Starting Stack Builder...");
		if(args !=null){
			log.info("args[] length="+args.length);
			for(String arg: args){
				log.info(arg);
			}
		}
	}

}
