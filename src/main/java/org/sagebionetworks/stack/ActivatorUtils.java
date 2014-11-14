package org.sagebionetworks.stack;

import java.util.HashMap;
import java.util.Map;

public class ActivatorUtils {
	/**
	 * Backend generic CNAME is always 'repo-<role>.prod.sagebase.org'
	 *		where <role> is either 'staging' or 'prod'
	 * 
	 * @param svcPrefix
	 * @param instanceRole
	 * @param subDomainName
	 * @param domainName
	 * @return 
	 */
	public static String getBackEndGenericCNAME(String instanceRole, String subDomainName, String domainName) {
		String s = String.format("repo-%s.%s.%s", instanceRole, subDomainName, domainName);
		return s;
	}
	
	/**
	 *	Backend instance is always 'repo-prod-<instance>.prod.sagebase.org'
	 *		where <instance> is <instance_num>-<beanstalk_num>
	 *	Keeping subdomain and domain as parameters for now
	 *	Independent of instanceRole
	 * 
	 * @param svcPrefix
	 * @param instanceRole
	 * @param stackInstance
	 * @param subDomainName
	 * @param domainName
	 * @return 
	 */
	public static String getBackEndInstanceCNAME(String instanceRole, String stackInstance, String subDomainName, String domainName) {
		return String.format("repo-prod-%s.%s.%s", stackInstance, subDomainName, domainName);
	}
	
	/**
	 * Portal generic name is either 'synapse-prod.synapse.org' (prod) or 'synapse-staging.synapse.org' (staging)
	 * @param instanceRole
	 * @return 
	 */
	public static String getPortalGenericCNAME(String instanceRole) {
		if ((!"prod".equals(instanceRole)) && (!"staging".equals(instanceRole))) {
			throw new IllegalArgumentException("InstanceRole must be 'prod' or 'staging'.");
		}
		return String.format("synapse-%s.synapse.org", instanceRole);
	}
	
	/**
	 * Portal instance CNAME is always 'portal-prod-<instance>.prod.sagebase.org'
	 *	where <instance> is <stack_number>-<beanstalk_number>
	 * 
	 * @param svcPrefix
	 * @param stackInstance
	 * @param domainName
	 * @return 
	 */
	public static String getPortalInstanceCNAME(String stackInstance, String subDomainName, String domainName) {
		return String.format("portal-prod-%s.%s.%s", stackInstance, subDomainName, domainName);
	}
	
	public static Map<String, String> mapBackendGenericCNAMEToInstanceCNAME(String instanceRole, String stackInstance) {
		Map<String, String> mapGenericToInstanceNames = new HashMap<String, String>();
		mapGenericToInstanceNames.put(
			ActivatorUtils.getBackEndGenericCNAME(instanceRole, Constants.R53_SUBDOMAIN_NAME, Constants.R53_BACKEND_HOSTEDZONE_NAME),
			ActivatorUtils.getBackEndInstanceCNAME(instanceRole, stackInstance, "prod", Constants.R53_BACKEND_HOSTEDZONE_NAME));
		return mapGenericToInstanceNames;
	}
	
	public static Map<String, String> mapPortalGenericCNAMEToInstanceCNAME(String instanceRole, String stackInstance) {
		Map<String, String> mapGenericToInstanceNames = new HashMap<String, String>();
		mapGenericToInstanceNames.put(
			ActivatorUtils.getPortalGenericCNAME(instanceRole),
			ActivatorUtils.getPortalInstanceCNAME(stackInstance, Constants.R53_SUBDOMAIN_NAME, Constants.R53_BACKEND_HOSTEDZONE_NAME));
		return mapGenericToInstanceNames;
	}
}
