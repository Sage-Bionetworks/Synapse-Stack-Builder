package org.sagebionetworks.stack;

import java.io.IOException;
import java.util.Properties;

import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.identitymanagement.model.ServerCertificateMetadata;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.Endpoint;
import com.amazonaws.services.cloudsearch.model.DomainStatus;
import com.amazonaws.services.cloudsearch.model.ServiceEndpoint;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.sagebionetworks.stack.Constants.*;

/**
 * Helper used by tests for setup.
 * 
 * @author John
 *
 */
public class TestHelper {


	/**
	 * Create a test Config.
	 * @return
	 * @throws IOException
	 */
	public static InputConfiguration createTestConfig(String stack) throws IOException{
		Properties inputProperties = createInputProperties(stack);
		InputConfiguration config = new InputConfiguration(inputProperties);
		Properties defaults = createDefaultProperties();
		config.addPropertiesWithPlaintext(defaults);
		// Add the SSL ARN
//		config.setSSLCertificateARN("generic", "ssl:arn:123:456");
//		config.setSSLCertificateARN("portal", "ssl:arn:123:456");
		return config;
	}

	/**
	 * @return
	 */
	public static Properties createDefaultProperties() {
		Properties defaults = new Properties();
		defaults.put(Constants.KEY_DEFAULT_ID_GEN_PASSWORD_PLAIN_TEXT, "id gen password");
		defaults.put(Constants.KEY_DEFAULT_STACK_INSTANCES_DB_PASSWORD_PLAIN_TEXT, "stack db password");
		defaults.put(Constants.KEY_CIDR_FOR_SSH, "255.255.255/1");
		defaults.put(Constants.KEY_RDS_ALAERT_SUBSCRIPTION_ENDPONT, "dev@sagebaser.org");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_MAIL_PW_PLAINTEXT, "mail password");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_BCC_GOOGLEAPPS_OAUTH_CONSUMER_SECRET_PLAINTEX, "google consumer oath key");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_BCC_GOOGLEAPPS_OAUTH_ACCESS_TOKEN_PLAINTEXT, "google access token");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_BCC_GOOGLEAPPS_OAUTH_ACCESS_TOKEN_SECRET_PLAINTEXT, "google access token secret");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_PORTAL_API_LINKEDIN_KEY, "linkedin key");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_PORTAL_API_LINKEDIN_SECRET_PLAINTEXT, "linkedin secret");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_PORTAL_API_GETSATISFACTION_KEY, "getsatisfaction key");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_PORTAL_API_GETSATISFACTION_SECRET_PLAINTEXT, "getsatisfaction secret");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_EZID_USERNAME, "ezid user");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_EZID_PASSWORD_PLAINTEXT, "ezid password");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_EZID_DOI_PREFIX, "doi prefix");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_REPO_MANAGER_JIRA_USER_PASSWORD_PLAINTEXT, "jira password");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_MIGRATION_API_KEY, "migrationAPIKey");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_SEARCH_ENABLED, "true");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_DYNAMO_ENABLED, "true");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_TABLE_ENABLED, "true");
		defaults.put(KEY_ORG_SAGEBIONETWORKS_NOTIFICATION_EMAIL_ADDRESS, "email@address.com");

		
		return defaults;
	}

	/**
	 * Create test input properties.
	 * @param stack
	 * @return
	 */
	public static Properties createInputProperties(String stack) {
		Properties inputProperties = new Properties();
		inputProperties.put(Constants.AWS_ACCESS_KEY, "AWS access key value");
		inputProperties.put(Constants.AWS_SECRET_KEY, "AWS secrite key value");
		inputProperties.put(Constants.STACK_ENCRYPTION_KEY, "Encryption key that is long enough");
		inputProperties.put(Constants.STACK, stack);
		inputProperties.put(Constants.INSTANCE, "A");
		inputProperties.put(Constants.PORTAL_BEANSTALK_NUMBER, "0");
		inputProperties.put(Constants.BRIDGE_BEANSTALK_NUMBER, "1");
		inputProperties.put(Constants.PLFM_BEANSTALK_NUMBER, "1");
		inputProperties.put(Constants.SWC_VERSION, "2.4.8");
		inputProperties.put(Constants.PLFM_VERSION, "1.2.3");
		inputProperties.put(Constants.BRIDGE_VERSION, "7.8.9");
		inputProperties.put(Constants.NUMBER_TABLE_INSTANCES, "2");
		return inputProperties;
	}
	
	/**
	 * Create GeneratedResources that can be used for a test.
	 * 
	 * @param config
	 * @return
	 */
	public static GeneratedResources createTestResources(InputConfiguration config){
		GeneratedResources resources  = new GeneratedResources();
		resources.setIdGeneratorDatabase(new DBInstance().withDBInstanceIdentifier(config.getIdGeneratorDatabaseIdentifier()).withEndpoint(new Endpoint().withAddress("id-gen-db.someplace.com")));
		resources.setStackInstancesDatabase(new DBInstance().withDBInstanceIdentifier(config.getStackInstanceDatabaseIdentifier()).withEndpoint(new Endpoint().withAddress("stack-instance-db.someplace.com")));
		resources.setSearchDomain(new DomainStatus().withSearchService(new ServiceEndpoint().withEndpoint("search-service.someplace.com")));
		resources.getSearchDomain().setDocService(new ServiceEndpoint().withEndpoint("doc-service.someplace.com"));
		resources.setSslCertificate("generic", new ServerCertificateMetadata().withArn("ssl:arn:123"));
		resources.setSslCertificate("portal", new ServerCertificateMetadata().withArn("ssl:arn:456"));
		resources.setSslCertificate("bridge", new ServerCertificateMetadata().withArn("ssl:arn:456"));
		resources.setPortalApplicationVersion(new ApplicationVersionDescription().withVersionLabel(config.getVersionLabel(PREFIX_PORTAL)));
		resources.setBridgeApplicationVersion(new ApplicationVersionDescription().withVersionLabel(config.getVersionLabel(PREFIX_BRIDGE)));
		resources.setRepoApplicationVersion(new ApplicationVersionDescription().withVersionLabel(config.getVersionLabel(PREFIX_REPO)));
		resources.setWorkersApplicationVersion(new ApplicationVersionDescription().withVersionLabel(config.getVersionLabel(PREFIX_WORKERS)));
		resources.setStackKeyPair(new KeyPairInfo().withKeyName(config.getStackKeyPairName()));
		return resources;
	}
	
	public static InputConfiguration createRoute53TestConfig(String stack) throws IOException {
		Properties inputProperties = createInputProperties(stack);
		InputConfiguration config = new InputConfiguration(inputProperties);
		Properties defaultProperties = createDefaultProperties();
		Map<String, String> cnameProps = getSvcCNAMEsProps(stack, Arrays.asList(Constants.PREFIX_BRIDGE, Constants.PREFIX_PORTAL, Constants.PREFIX_REPO, Constants.PREFIX_WORKERS));
		defaultProperties.putAll(cnameProps);
		defaultProperties.put("stack.subdomain", stack+".sagebase.org");
		config.addPropertiesWithPlaintext(defaultProperties);
		return config;
	}
	
	public static Map<ListResourceRecordSetsRequest, ListResourceRecordSetsResult> createListExpectedListResourceRecordSetsRequestAllFound(String stack) {
		Map<ListResourceRecordSetsRequest, ListResourceRecordSetsResult> m = new HashMap<ListResourceRecordSetsRequest, ListResourceRecordSetsResult>();
		List<String> svcPrefixes = Arrays.asList(Constants.PREFIX_BRIDGE, Constants.PREFIX_PORTAL, Constants.PREFIX_REPO, Constants.PREFIX_WORKERS);
		Map<String, String> map = getSvcCNAMEsProps(stack, svcPrefixes);
		for (String svcPrefix: svcPrefixes) {
			ListResourceRecordSetsRequest req = new ListResourceRecordSetsRequest().withStartRecordType(RRType.CNAME).withStartRecordName(map.get(svcPrefix + ".service.environment.subdomain.cname")).withMaxItems("1");
			ResourceRecord rr = new ResourceRecord().withValue(map.get(svcPrefix + ".service.environment.cname.prefix") + ".elasticbeanstalk.com");
			ListResourceRecordSetsResult res = new ListResourceRecordSetsResult().withResourceRecordSets(new ResourceRecordSet().withName(map.get(svcPrefix + ".service.environment.subdomain.cname")).withTTL(300L).withType(RRType.CNAME).withResourceRecords(rr));
			m.put(req, res);
		}
		return m;
	}
	
	
	public static Map<ListResourceRecordSetsRequest, ListResourceRecordSetsResult> createListExpectedListResourceRecordSetsRequestNoneFound(String stack) {
		Map<ListResourceRecordSetsRequest, ListResourceRecordSetsResult> m = new HashMap<ListResourceRecordSetsRequest, ListResourceRecordSetsResult>();
		// For Auth and Portal, simulate 'not last' situation i.e. the next record is returned
		List<String> svcPrefixes = Arrays.asList(Constants.PREFIX_PORTAL);
		Map<String, String> map = getSvcCNAMEsProps(stack, svcPrefixes);
		for (String svcPrefix: svcPrefixes) {
			ListResourceRecordSetsRequest req = new ListResourceRecordSetsRequest().withStartRecordType(RRType.CNAME).withStartRecordName(map.get(svcPrefix + ".service.environment.subdomain.cname")).withMaxItems("1");
			ResourceRecord rr = new ResourceRecord().withValue(map.get(svcPrefix + ".service.environment.cname.prefix") + "2.elasticbeanstalk.com");
			ListResourceRecordSetsResult res = new ListResourceRecordSetsResult().withResourceRecordSets(new ResourceRecordSet().withName(map.get(svcPrefix + ".service.environment.subdomain.cname") + "2").withTTL(300L).withType(RRType.CNAME).withResourceRecords(rr));
			m.put(req, res);
		}
		// For Repo and Workers, simulate 'last' situation i.e. no record is returned
		svcPrefixes = Arrays.asList(Constants.PREFIX_REPO, Constants.PREFIX_WORKERS);
		map = getSvcCNAMEsProps(stack, svcPrefixes);
		for (String svcPrefix: svcPrefixes) {
			ListResourceRecordSetsRequest req = new ListResourceRecordSetsRequest().withStartRecordType(RRType.CNAME).withStartRecordName(map.get(svcPrefix + ".service.environment.subdomain.cname")).withMaxItems("1");
			ResourceRecord rr = null;
			ListResourceRecordSetsResult res = new ListResourceRecordSetsResult().withResourceRecordSets(new ArrayList<ResourceRecordSet>());
			m.put(req, res);
		}
		return m;
	}
	
	private static Map<String, String> getSvcCNAMEsProps(String stack, List<String> svcPrefixes) {
		Map<String, String> cnameProps = new HashMap<String, String>();
		for (String svcPrefix: svcPrefixes) {
			cnameProps.put(svcPrefix + ".service.environment.subdomain.cname", svcPrefix + "." + stack + ".inst.r53.sagebase.org");
			cnameProps.put(svcPrefix + ".service.environment.cname.prefix",  svcPrefix + "-" + stack + "-inst-sagebase-org");
		}
		return cnameProps;
	}

}
