package org.sagebionetworks.stack;

import com.amazonaws.services.cloudsearch.AmazonCloudSearchClient;
import com.amazonaws.services.cloudsearch.model.CreateDomainRequest;
import com.amazonaws.services.cloudsearch.model.CreateDomainResult;
import com.amazonaws.services.cloudsearch.model.DeleteDomainRequest;
import com.amazonaws.services.cloudsearch.model.DeleteDomainResult;
import com.amazonaws.services.cloudsearch.model.DescribeDomainsRequest;
import com.amazonaws.services.cloudsearch.model.DescribeDomainsResult;
import com.amazonaws.services.cloudsearch.model.DomainStatus;
import com.amazonaws.services.simpleworkflow.model.DescribeDomainRequest;
import java.io.IOException;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;


import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.config.InputConfiguration;
/**
 *
 * @author xavier
 */
public class SearchIndexSetupTest {
	
	InputConfiguration config;
	GeneratedResources resources;
	MockAmazonClientFactory factory = new MockAmazonClientFactory();
	AmazonCloudSearchClient mockClient;

	@Before
	public void before() throws IOException{
		config = TestHelper.createTestConfig("dev");
		mockClient = factory.createCloudSearchClient();
		resources = new GeneratedResources();
	}
	
	@Test
	public void testSetupResourcesExistentDomain() {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources);
		DomainStatus domainStatus = new DomainStatus().withDomainName(expectedDomainName);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(new DescribeDomainsResult().withDomainStatusList(domainStatus));
		when(mockClient.createDomain(any(CreateDomainRequest.class))).thenReturn(new CreateDomainResult().withDomainStatus(domainStatus));
		idx.setupResources();
		assertNotNull(resources.getSearchDomain());
		assertEquals(expectedDomainName, resources.getSearchDomain().getDomainName());
	}
	
	@Test
	public void testSetupResourcesNonExistentDomain() {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources);
		DomainStatus domainStatus = new DomainStatus().withDomainName(expectedDomainName);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(null, new DescribeDomainsResult().withDomainStatusList(domainStatus));
		when(mockClient.createDomain(any(CreateDomainRequest.class))).thenReturn(new CreateDomainResult().withDomainStatus(domainStatus));
		idx.setupResources();
		assertNotNull(resources.getSearchDomain());
		assertEquals(expectedDomainName, resources.getSearchDomain().getDomainName());
	}
	
	@Test
	public void testTeardownResourcesExistentDomain() {
		String deletedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources);
		DomainStatus domainStatus = new DomainStatus().withDomainName(deletedDomainName);
		resources.setSearchDomain(domainStatus);
		DeleteDomainRequest delReq = new DeleteDomainRequest().withDomainName(deletedDomainName);
		when(mockClient.deleteDomain(delReq)).thenReturn(new DeleteDomainResult().withDomainStatus(domainStatus));
		idx.teardownResources();
		assertNull(resources.getSearchDomain());
	}
	
	
	@Test
	public void testDescribeResourcesExistentDomain() {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources);
		DomainStatus domainStatus = new DomainStatus().withDomainName(expectedDomainName);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(new DescribeDomainsResult().withDomainStatusList(domainStatus));
		idx.describeResources();
		assertNotNull(resources.getSearchDomain());
		assertEquals(expectedDomainName, resources.getSearchDomain().getDomainName());
	}
	
	@Test
	public void testDescribeResourcesNonExistentDomain() {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources);
		DomainStatus domainStatus = new DomainStatus().withDomainName(expectedDomainName);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(null);
		idx.describeResources();
		assertNull(resources.getSearchDomain());
	}
}
