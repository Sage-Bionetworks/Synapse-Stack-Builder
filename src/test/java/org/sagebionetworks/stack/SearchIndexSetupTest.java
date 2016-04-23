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
import static org.mockito.Matchers.anyLong;
import org.mockito.Mockito;
import static org.mockito.Mockito.doNothing;


import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.util.Sleeper;
/**
 *
 * @author xavier
 */
public class SearchIndexSetupTest {
	
	InputConfiguration config;
	GeneratedResources resources;
	MockAmazonClientFactory factory = new MockAmazonClientFactory();
	AmazonCloudSearchClient mockClient;
	private Sleeper mockSleeper;

	@Before
	public void before() throws IOException{
		config = TestHelper.createTestConfig("dev");
		mockClient = factory.createCloudSearchClient();
		resources = new GeneratedResources();
		mockSleeper = Mockito.mock(Sleeper.class);
	}
	
	@Test
	public void testSetupResourcesExistentDomain() {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources, mockSleeper);
		DomainStatus domainStatus = new DomainStatus().withDomainName(expectedDomainName).withCreated(Boolean.TRUE).withProcessing(Boolean.FALSE);
		DescribeDomainsResult expectedRes = new DescribeDomainsResult().withDomainStatusList(domainStatus);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(expectedRes, expectedRes);
		when(mockClient.createDomain(any(CreateDomainRequest.class))).thenReturn(new CreateDomainResult().withDomainStatus(domainStatus));
		doNothing().when(mockSleeper).sleep(anyLong());
		idx.setupResources();
		assertNotNull(resources.getSearchDomain());
		assertEquals(expectedDomainName, resources.getSearchDomain().getDomainName());
	}
	
	@Test
	public void testSetupResourcesNonExistentDomain() {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources, mockSleeper);
		DomainStatus domainStatus = new DomainStatus().withDomainName(expectedDomainName).withCreated(Boolean.TRUE).withProcessing(Boolean.FALSE);
		DescribeDomainsResult expectedRes = new DescribeDomainsResult().withDomainStatusList(domainStatus);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(null, expectedRes, expectedRes);
		when(mockClient.createDomain(any(CreateDomainRequest.class))).thenReturn(new CreateDomainResult().withDomainStatus(domainStatus));
		DescribeDomainsResult expectedDescribeRes = new DescribeDomainsResult().withDomainStatusList(domainStatus);
		doNothing().when(mockSleeper).sleep(anyLong());
		idx.setupResources();
		assertNotNull(resources.getSearchDomain());
		assertEquals(expectedDomainName, resources.getSearchDomain().getDomainName());
	}
	
	@Test
	public void testDescribeResourcesExistentDomain() {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources, mockSleeper);
		DomainStatus domainStatus = new DomainStatus().withDomainName(expectedDomainName);
		DescribeDomainsResult expectedRes = new DescribeDomainsResult().withDomainStatusList(domainStatus);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(expectedRes);
		doNothing().when(mockSleeper).sleep(anyLong());
		idx.describeResources();
		assertNotNull(resources.getSearchDomain());
		assertEquals(expectedDomainName, resources.getSearchDomain().getDomainName());
	}
	
	@Test
	public void testDescribeResourcesNonExistentDomain() {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources, mockSleeper);
		DomainStatus domainStatus = new DomainStatus().withDomainName(expectedDomainName);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(null);
		doNothing().when(mockSleeper).sleep(anyLong());
		idx.describeResources();
		assertNull(resources.getSearchDomain());
	}
}
