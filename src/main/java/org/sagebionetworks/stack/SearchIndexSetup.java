package org.sagebionetworks.stack;

import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.cloudsearch.AmazonCloudSearchClient;
import com.amazonaws.services.cloudsearch.model.CreateDomainRequest;
import com.amazonaws.services.cloudsearch.model.CreateDomainResult;
import com.amazonaws.services.cloudsearch.model.DeleteDomainRequest;
import com.amazonaws.services.cloudsearch.model.DeleteDomainResult;
import com.amazonaws.services.cloudsearch.model.DescribeDomainsRequest;
import com.amazonaws.services.cloudsearch.model.DescribeDomainsResult;
import com.amazonaws.services.cloudsearch.model.DomainStatus;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

/**
 * Setup the Cloud search index for this stack isntance.
 * @author John
 *
 */
public class SearchIndexSetup implements ResourceProcessor {
	
	private static Logger log = Logger.getLogger(SearchIndexSetup.class.getName());
	
	AmazonCloudSearchClient client;
	InputConfiguration config;
	GeneratedResources resources;
	public SearchIndexSetup(AmazonClientFactory factory,
			InputConfiguration config, GeneratedResources resources) {
		super();
		this.initialize(factory, config, resources);
	}
	
	public void initialize(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		this.client = factory.createCloudSearchClient();
		this.config = config;
		this.resources = resources;
	}
	
	public void setupResources() {
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
	
	public void teardownResources() {
		String domainName = null;
		DomainStatus domainStatus = resources.getSearchDomain();
		DeleteDomainResult result = null;
		if (domainStatus != null) {
			domainName = domainStatus.getDomainName();
			result = client.deleteDomain(new DeleteDomainRequest().withDomainName(domainName));
		}
		if (result != null) {
			this.resources.setSearchDomain(null);
		} else {
			throw new IllegalStateException("Could not delete domain " + domainName);
		}
	}
	
	public void describeResources() {
		String domainName = config.getSearchIndexDomainName();
		DomainStatus domain = describeDomains();
		if (domain != null) {
			this.resources.setSearchDomain(domain);
		}
	}
	
	public void setupSearch(){
	}
	
	private DomainStatus describeDomains(){
		DescribeDomainsResult result = client.describeDomains(new DescribeDomainsRequest().withDomainNames(config.getSearchIndexDomainName()));
		if((result != null) && (result.getDomainStatusList().size() == 1)) return result.getDomainStatusList().get(0);
		return null;
	}

}
