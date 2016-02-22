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
		DomainStatus domain = getDomainStatus(domainName);
		if(domain == null){
			// We need to create it.
			log.debug(String.format("Search index domain: '%1$s' does not exist, so creating it...",domainName));
			CreateDomainResult result = client.createDomain(new CreateDomainRequest().withDomainName(domainName));
			domain = getDomainStatus(domainName);
			this.resources.setSearchDomain(domain);
		}else{
			// It already exists
			log.debug(String.format("Search index domain: '%1$s' already exists.", domainName));
			this.resources.setSearchDomain(domain);
		}

		DomainStatus ds = waitForSearchDomain(domainName);
		if (ds == null) {
			throw new RuntimeException("Search domain not available yet");
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
		}
	}
	
	public void describeResources() {
		String domainName = config.getSearchIndexDomainName();
		DomainStatus domain = getDomainStatus(domainName);
		if (domain != null) {
			this.resources.setSearchDomain(domain);
		}
	}
	
	public void setupSearch(){
	}
	
	private DomainStatus getDomainStatus(String domainName){
		DescribeDomainsResult result = client.describeDomains(new DescribeDomainsRequest().withDomainNames(domainName));
		if ((result != null) && (result.getDomainStatusList().size() == 1)) {
			return result.getDomainStatusList().get(0);
		} else {
			return null;
		}
	}
	
	private DomainStatus waitForSearchDomain(String domainName) {
		DomainStatus domainStatus = null;
		boolean available = false;
		int numSuccesses = 0;		
		for (int i =0; i < 3; i++) {
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			domainStatus = getDomainStatus(domainName);
			available = domainStatus.isCreated() && (!domainStatus.isProcessing());
			if (available) {
				numSuccesses++;
			}
		}
		if (available && (numSuccesses >= 2)) {
			return domainStatus;
		} else {
			return null;
		}
	}

}
