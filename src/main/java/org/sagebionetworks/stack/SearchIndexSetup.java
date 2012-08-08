package org.sagebionetworks.stack;

import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.cloudsearch.AmazonCloudSearchClient;
import com.amazonaws.services.cloudsearch.model.CreateDomainRequest;
import com.amazonaws.services.cloudsearch.model.CreateDomainResult;
import com.amazonaws.services.cloudsearch.model.DescribeDomainsRequest;
import com.amazonaws.services.cloudsearch.model.DescribeDomainsResult;
import com.amazonaws.services.cloudsearch.model.DomainStatus;

/**
 * Setup the Cloud search index for this stack isntance.
 * @author John
 *
 */
public class SearchIndexSetup {
	
	private static Logger log = Logger.getLogger(SearchIndexSetup.class.getName());
	
	AmazonCloudSearchClient client;
	InputConfiguration config;
	GeneratedResources resources;
	public SearchIndexSetup(AmazonCloudSearchClient client,
			InputConfiguration config, GeneratedResources resources) {
		super();
		this.client = client;
		this.config = config;
		this.resources = resources;
	}
	
	
	public void setupSearch(){
		String domainName = config.getSearchIndexDomainName();
		// Does this search domain exist?
		DomainStatus domain = describeDomains();
		if(domain == null){
			// We need to create it.
			log.debug(String.format("Search index domain: '%1$s' does not exist, so creating it...",domainName));
			CreateDomainResult result = client.createDomain(new CreateDomainRequest().withDomainName(domainName));
			domain = describeDomains();
			this.resources.setSearchDomain(domain);
		}else{
			// It already exists
			log.debug(String.format("Search index domain: '%1$s' already exists.", domainName));
			this.resources.setSearchDomain(domain);
		}
		
	}
	
	private DomainStatus describeDomains(){
		DescribeDomainsResult result = client.describeDomains(new DescribeDomainsRequest().withDomainNames(config.getSearchIndexDomainName()));
		if(result.getDomainStatusList().size() == 1) return result.getDomainStatusList().get(0);
		return null;
	}

}
