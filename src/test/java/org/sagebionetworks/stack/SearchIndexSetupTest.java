package org.sagebionetworks.stack;

import com.amazonaws.services.cloudsearchv2.AmazonCloudSearchClient;
import com.amazonaws.services.cloudsearchv2.model.*;
import com.amazonaws.services.simpleworkflow.model.DescribeDomainRequest;
import java.io.IOException;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

import org.mockito.*;


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

	@Mock
	private Sleeper mockSleeper;

	@Captor
	private ArgumentCaptor<UpdateScalingParametersRequest> scalingParamsReqCaptor;

	@Captor
	private ArgumentCaptor<CreateDomainRequest> createDomainReqCaptor;

	@Before
	public void before() throws IOException {
		MockitoAnnotations.initMocks(this);

		config = TestHelper.createTestConfig("dev");
		mockClient = factory.createCloudSearchClient();
		resources = new GeneratedResources();
	}
	
	@Test
	public void testSetupResourcesExistentDomain() throws Exception {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources, mockSleeper);
		// Return a domain that is active
		DomainStatus domainStatus = new DomainStatus().withDomainName(expectedDomainName).withCreated(Boolean.TRUE).withProcessing(Boolean.FALSE);
		DescribeDomainsResult expectedRes = new DescribeDomainsResult().withDomainStatusList(domainStatus);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(expectedRes);
		// Expected scaling params request
		ScalingParameters expectedScalingParams = new ScalingParameters().withDesiredInstanceType(PartitionInstanceType.SearchM3Large).withDesiredReplicationCount(1);
		UpdateScalingParametersRequest expectedReq = new UpdateScalingParametersRequest().withDomainName(expectedDomainName).withScalingParameters(expectedScalingParams);
		// Return the scaling params
		ScalingParametersStatus expectedScalingParamsStatus = new ScalingParametersStatus().withOptions(expectedScalingParams);
		when(mockClient.updateScalingParameters(any(UpdateScalingParametersRequest.class))).thenReturn(new UpdateScalingParametersResult().withScalingParameters(expectedScalingParamsStatus));

		// Call under test
		idx.setupResources();

		assertNotNull(resources.getSearchDomain());
		assertEquals(expectedDomainName, resources.getSearchDomain().getDomainName());
		verify(mockClient).describeDomains(any(DescribeDomainsRequest.class));
		verify(mockClient).updateScalingParameters(scalingParamsReqCaptor.capture());
		assertEquals(expectedReq, scalingParamsReqCaptor.getValue());
		verify(mockSleeper, never()).sleep(anyLong());
	}

	// TODO: Delete this test or modify if it turns out one cannot update the scaling params while the domain is processing
	@Ignore
	@Test
	public void testSetupResourcesExistentDomainNotActive() throws Exception {
		String expectedDomainName = config.getSearchIndexDomainName();
		SearchIndexSetup idx = new SearchIndexSetup(factory, config, resources, mockSleeper);
		// Return a domain that's processing
		DomainStatus domainStatusProcessing = new DomainStatus().withDomainName(expectedDomainName).withCreated(Boolean.TRUE).withProcessing(Boolean.TRUE);
		DescribeDomainsResult ddRes = new DescribeDomainsResult().withDomainStatusList(domainStatusProcessing);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(ddRes);
		// Expected scaling params request
		ScalingParameters expectedScalingParams = new ScalingParameters().withDesiredInstanceType(PartitionInstanceType.SearchM3Large).withDesiredReplicationCount(1);
		UpdateScalingParametersRequest expectedReq = new UpdateScalingParametersRequest().withDomainName(expectedDomainName).withScalingParameters(expectedScalingParams);
		// Return the scaling params
		ScalingParametersStatus expectedScalingParamsStatus = new ScalingParametersStatus().withOptions(expectedScalingParams);
		when(mockClient.updateScalingParameters(any(UpdateScalingParametersRequest.class))).thenReturn(new UpdateScalingParametersResult().withScalingParameters(expectedScalingParamsStatus));

		// Call under test
		idx.setupResources();

		assertNotNull(resources.getSearchDomain());
		assertEquals(expectedDomainName, resources.getSearchDomain().getDomainName());
		verify(mockClient).describeDomains(any(DescribeDomainsRequest.class));
		verify(mockClient).updateScalingParameters(scalingParamsReqCaptor.capture());
		assertEquals(expectedReq, scalingParamsReqCaptor.getValue());
		verify(mockSleeper, never()).sleep(anyLong());
	}

	// TODO: This test was to check if we were getting out of waiting for domain. Not needed anymore
	@Ignore
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
		// Eventually return a domain that's active
		DomainStatus domainStatus = new DomainStatus().withDomainName(expectedDomainName).withCreated(Boolean.TRUE).withProcessing(Boolean.FALSE);
		DescribeDomainsResult expectedRes = new DescribeDomainsResult().withDomainStatusList(domainStatus);
		when(mockClient.describeDomains(any(DescribeDomainsRequest.class))).thenReturn(null, expectedRes);
		// Expected CreateDomainRequest
		CreateDomainRequest expectedCdReq = new CreateDomainRequest().withDomainName(expectedDomainName);
		when(mockClient.createDomain(any(CreateDomainRequest.class))).thenReturn(new CreateDomainResult().withDomainStatus(domainStatus));
		DescribeDomainsResult expectedDescribeRes = new DescribeDomainsResult().withDomainStatusList(domainStatus);

		// Expected scaling params request
		ScalingParameters expectedScalingParams = new ScalingParameters().withDesiredInstanceType(PartitionInstanceType.SearchM3Large).withDesiredReplicationCount(1);
		UpdateScalingParametersRequest expectedReq = new UpdateScalingParametersRequest().withDomainName(expectedDomainName).withScalingParameters(expectedScalingParams);
		// Return the scaling params
		ScalingParametersStatus expectedScalingParamsStatus = new ScalingParametersStatus().withOptions(expectedScalingParams);
		when(mockClient.updateScalingParameters(any(UpdateScalingParametersRequest.class))).thenReturn(new UpdateScalingParametersResult().withScalingParameters(expectedScalingParamsStatus));

		// Call under test
		idx.setupResources();

		assertNotNull(resources.getSearchDomain());
		assertEquals(expectedDomainName, resources.getSearchDomain().getDomainName());
		verify(mockClient, times(2)).describeDomains(any(DescribeDomainsRequest.class));
		verify(mockClient).createDomain(createDomainReqCaptor.capture());
		assertEquals(expectedCdReq, createDomainReqCaptor.getValue());
		verify(mockClient).updateScalingParameters(scalingParamsReqCaptor.capture());
		assertEquals(expectedReq, scalingParamsReqCaptor.getValue());
		verify(mockSleeper, never()).sleep(anyLong());
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
