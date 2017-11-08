package org.sagebionetworks.stack;

import com.amazonaws.services.cloudsearchv2.AmazonCloudSearchClient;
import com.amazonaws.services.cloudsearchv2.model.CreateDomainRequest;
import com.amazonaws.services.cloudsearchv2.model.CreateDomainResult;
import com.amazonaws.services.cloudsearchv2.model.DeleteDomainRequest;
import com.amazonaws.services.cloudsearchv2.model.DeleteDomainResult;
import com.amazonaws.services.cloudsearchv2.model.DescribeDomainsRequest;
import com.amazonaws.services.cloudsearchv2.model.DescribeDomainsResult;
import com.amazonaws.services.cloudsearchv2.model.DomainStatus;
import com.amazonaws.services.simpleworkflow.model.DescribeDomainRequest;
import java.io.IOException;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

import org.mockito.Mockito;


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
	public void testSetupResourcesExistentDomain() throws Exception {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources, mockSleeper);
		DomainStatus domainStatus = new DomainStatus().withDomainName(expectedDomainName).withCreated(Boolean.TRUE).withProcessing(Boolean.FALSE);
		DescribeDomainsResult expectedRes = new DescribeDomainsResult().withDomainStatusList(domainStatus);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(expectedRes);
		when(mockClient.createDomain(any(CreateDomainRequest.class))).thenReturn(new CreateDomainResult().withDomainStatus(domainStatus));

		// Call under test
		idx.setupResources();

		assertNotNull(resources.getSearchDomain());
		assertEquals(expectedDomainName, resources.getSearchDomain().getDomainName());
		verify(mockClient, times(5)).describeDomains(any(DescribeDomainsRequest.class));
		verify(mockSleeper, times(4)).sleep(anyLong());
	}

	@Test
	public void testSetupResourcesExistentDomainNotActive() throws Exception {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources, mockSleeper);
		DomainStatus domainStatusProcessing = new DomainStatus().withDomainName(expectedDomainName).withCreated(Boolean.TRUE).withProcessing(Boolean.TRUE);
		DescribeDomainsResult expectedRes1 = new DescribeDomainsResult().withDomainStatusList(domainStatusProcessing);
		DomainStatus domainStatusActive = new DomainStatus().withDomainName(expectedDomainName).withCreated(Boolean.TRUE).withProcessing(Boolean.FALSE);
		DescribeDomainsResult expectedRes2 = new DescribeDomainsResult().withDomainStatusList(domainStatusActive);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(expectedRes1, expectedRes1, expectedRes2);
		when(mockClient.createDomain(any(CreateDomainRequest.class))).thenReturn(new CreateDomainResult().withDomainStatus(domainStatusProcessing));

		// Call under test
		idx.setupResources();

		assertNotNull(resources.getSearchDomain());
		assertEquals(expectedDomainName, resources.getSearchDomain().getDomainName());
		verify(mockClient, times(6)).describeDomains(any(DescribeDomainsRequest.class));
		verify(mockSleeper, times(5)).sleep(anyLong());
	}

	@Test(expected=RuntimeException.class)
	public void testSetupResourcesExistentDomainNeverActive() throws Exception {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources, mockSleeper);
		DomainStatus domainStatusProcessing = new DomainStatus().withDomainName(expectedDomainName).withCreated(Boolean.TRUE).withProcessing(Boolean.TRUE);
		DescribeDomainsResult expectedRes1 = new DescribeDomainsResult().withDomainStatusList(domainStatusProcessing);
		DomainStatus domainStatusActive = new DomainStatus().withDomainName(expectedDomainName).withCreated(Boolean.TRUE).withProcessing(Boolean.FALSE);
		DescribeDomainsResult expectedRes2 = new DescribeDomainsResult().withDomainStatusList(domainStatusActive);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(expectedRes1);
		when(mockClient.createDomain(any(CreateDomainRequest.class))).thenReturn(new CreateDomainResult().withDomainStatus(domainStatusProcessing));

		// Call under test
		idx.setupResources();

		assertNotNull(resources.getSearchDomain());
		assertEquals(expectedDomainName, resources.getSearchDomain().getDomainName());
		verify(mockClient, times(11)).describeDomains(any(DescribeDomainsRequest.class));
		verify(mockSleeper, times(10)).sleep(anyLong());
	}

	@Test
	public void testSetupResourcesNonExistentDomain() throws Exception {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources, mockSleeper);
		DomainStatus domainStatus = new DomainStatus().withDomainName(expectedDomainName).withCreated(Boolean.TRUE).withProcessing(Boolean.FALSE);
		DescribeDomainsResult expectedRes = new DescribeDomainsResult().withDomainStatusList(domainStatus);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(null, expectedRes);
		when(mockClient.createDomain(any(CreateDomainRequest.class))).thenReturn(new CreateDomainResult().withDomainStatus(domainStatus));
		DescribeDomainsResult expectedDescribeRes = new DescribeDomainsResult().withDomainStatusList(domainStatus);

		// Call under test
		idx.setupResources();

		assertNotNull(resources.getSearchDomain());
		assertEquals(expectedDomainName, resources.getSearchDomain().getDomainName());
		verify(mockClient, times(6)).describeDomains(any(DescribeDomainsRequest.class));
		verify(mockSleeper, times(4)).sleep(anyLong());
	}
	
	@Test
	public void testDescribeResourcesExistentDomain() throws Exception {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources, mockSleeper);
		DomainStatus domainStatus = new DomainStatus().withDomainName(expectedDomainName);
		DescribeDomainsResult expectedRes = new DescribeDomainsResult().withDomainStatusList(domainStatus);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(expectedRes);

		// Call under test
		idx.describeResources();

		assertNotNull(resources.getSearchDomain());
		assertEquals(expectedDomainName, resources.getSearchDomain().getDomainName());
		verify(mockSleeper, never()).sleep(anyLong());
	}
	
	@Test
	public void testDescribeResourcesNonExistentDomain() throws Exception {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources, mockSleeper);
		DomainStatus domainStatus = new DomainStatus().withDomainName(expectedDomainName);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(null);

		// Call under test
		idx.describeResources();

		assertNull(resources.getSearchDomain());
		verify(mockSleeper, never()).sleep(anyLong());
	}
}
