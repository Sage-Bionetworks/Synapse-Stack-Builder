package org.sagebionetworks.stack;

import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.GetHostedZoneRequest;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.RRType;
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
public class StackSwapper {
	private static Logger log = Logger.getLogger(BuildStackMain.class.getName());
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		try {
			String srcStack, srcStackInstance, destStack, destStackInstance;
			String pathConfig;
			Properties inputProps = null;

			log.info("Staring StackSwapper...");

			// TODO: Better args checking
			if ((args != null) && (args.length == 5)) {
				srcStack = args[0];
				srcStackInstance = args[1];
				destStack = args[2];
				destStackInstance = args[3];
				pathConfig = args[4];

				if (pathConfig != null) {
					inputProps = loadPropertiesFromPath(pathConfig);
				} else {
					inputProps = System.getProperties();
				}
				
				swapStack(inputProps, srcStack, srcStackInstance, destStack, destStackInstance, new AmazonClientFactoryImpl());
			} else {
				throw new IllegalArgumentException("Wrong number of arguments!");
			}

			log.info("Exiting StackSwapper...");
		} catch (Throwable e) {
			log.error("Terminating: ",e);
		}finally{
			log.info("Terminating stack builder\n\n\n");
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
	
	private static void swapStack(Properties props, String srcStack, String srcStackInstance, String destStack, String destStackInstance, AmazonClientFactory factory) throws IOException {
		List<String> svcPrefixes = Arrays.asList(Constants.PREFIX_AUTH, Constants.PREFIX_PORTAL, Constants.PREFIX_REPO, Constants.PREFIX_SEARCH);
		String r53SubdomainName;
		
		InputConfiguration config = new InputConfiguration(props);
		factory.setCredentials(config.getAWSCredentials());
		
		AmazonRoute53Client client = factory.createRoute53Client();
		
		// Assume single hosted zone for now
		ListHostedZonesResult res = client.listHostedZones();
		HostedZone hz = res.getHostedZones().get(0);
		r53SubdomainName = hz.getName();
		
		for (String svcPrefix: svcPrefixes) {
			String srcSvcGenericCNAME = svcPrefix + "." + srcStack + "." + r53SubdomainName;
			String srcSvcCNAME = svcPrefix + "." + srcStack + "." + srcStackInstance + "." + r53SubdomainName;
			String destSvcGenericCNAME = svcPrefix + "." + destStack + "." + r53SubdomainName;
			String destSvcCNAME = svcPrefix + "." + destStack + "." + destStackInstance + "." + r53SubdomainName;
			
			// Change  srcSvcGenericCNAME record to point to destSvcCNAME
			ListResourceRecordSetsRequest req = new ListResourceRecordSetsRequest();
			req.setHostedZoneId(hz.getId());
			req.setStartRecordType(RRType.CNAME);
			req.setStartRecordName(svcPrefix);
			req.setMaxItems("1");
			ListResourceRecordSetsResult lrRes = client.listResourceRecordSets(req);
			ResourceRecordSet rrs = null;
			if ((lrRes.getResourceRecordSets().size() > 0) && (svcPrefix.equals(lrRes.getResourceRecordSets().get(0).getName()))) {
				rrs = lrRes.getResourceRecordSets().get(0);
			}
			if (rrs != null) {
				
			}
		}
	}
	
	private static Change CreateChange(HostedZone hz, String resourceRecordName, String newValue) {
		Change change = null;
		

		
		return change;
	}
}
