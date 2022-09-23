package org.sagebionetworks.template.repo.beanstalk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.sagebionetworks.template.repo.beanstalk.ElasticBeanstalkSolutionStackNameProviderImpl.AMI_VIRTUALIZATION_TYPE;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.CustomAmi;
import com.amazonaws.services.elasticbeanstalk.model.DescribePlatformVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribePlatformVersionResult;
import com.amazonaws.services.elasticbeanstalk.model.ListPlatformVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.ListPlatformVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.PlatformDescription;
import com.amazonaws.services.elasticbeanstalk.model.PlatformSummary;
import org.sagebionetworks.template.config.RepoConfiguration;

@ExtendWith(MockitoExtension.class)
public class ElasticBeanstalkSolutionStackNameProviderImplTest {
	@Mock
	AWSElasticBeanstalkClient mockElasticBeanstalkClient;

	@Mock
	AmazonEC2Client mockEc2Client;

	@Mock
	RepoConfiguration mockConfig;

	ElasticBeanstalkSolutionStackNameProviderImpl encrypter;

	final String originalImageId = "ami-originalId";
	final String platformArn = "platformArn123";
	final String solutionStackName = "Java -42 with Tomcat 9000.1 version 1.2.3";

	PlatformDescription platformDescription;

	@BeforeEach
	public void setUp(){
		encrypter = new ElasticBeanstalkSolutionStackNameProviderImpl(mockElasticBeanstalkClient, mockEc2Client, mockConfig);
		platformDescription = new PlatformDescription()
				.withSolutionStackName(solutionStackName)
				.withCustomAmiList(
						new CustomAmi()
								.withVirtualizationType("These are not the types you are looking for").withImageId("wrong1"),
						new CustomAmi()
								.withVirtualizationType(AMI_VIRTUALIZATION_TYPE).withImageId(originalImageId),
						new CustomAmi()
								.withVirtualizationType("Wrong again").withImageId("wrong2"));
	}

	@Test
	public void testGetSolutionStackName(){
		when(mockElasticBeanstalkClient.listPlatformVersions(any(ListPlatformVersionsRequest.class)))
				.thenReturn(new ListPlatformVersionsResult().withPlatformSummaryList(new PlatformSummary().withPlatformArn(platformArn)));
		when(mockElasticBeanstalkClient.describePlatformVersion(new DescribePlatformVersionRequest()
				.withPlatformArn(platformArn))).thenReturn(new DescribePlatformVersionResult().withPlatformDescription(platformDescription));

		String expectedSolutionStackName = solutionStackName;
		//method under test
		assertEquals(expectedSolutionStackName, encrypter.getSolutionStackName("tomcatVersion", "javaVersion", "linuxVersion"));
	}

	@Test
	public void testGetSolutionStackName_nullJavaVersion(){
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			encrypter.getPlatformArn(null, "9000.1", "1.2.3");
		});
	}

	@Test
	public void testGetSolutionStackName_nullAmazonLinuxVersion(){
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			encrypter.getPlatformArn("-42", "9000.1", null);
		});
	}

	@Test
	public void testGetSolutionStackName_nullTomcatVersion(){
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			encrypter.getPlatformArn("-42", null, "1.2.3");
		});
	}

	@Test
	public void testGetPlatformArn__noResults(){
		when(mockElasticBeanstalkClient.listPlatformVersions(any(ListPlatformVersionsRequest.class)))
				.thenReturn(new ListPlatformVersionsResult());

		//method under test
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			encrypter.getPlatformArn("1", "2", "3");
		});
	}

	@Test
	public void testGetPlatformArn__resultFound(){
		when(mockElasticBeanstalkClient.listPlatformVersions(any(ListPlatformVersionsRequest.class)))
			.thenReturn(new ListPlatformVersionsResult().withPlatformSummaryList(new PlatformSummary().withPlatformArn(platformArn)));
		//method under test
		String arnResult = encrypter.getPlatformArn("1", "2", "3");

		assertEquals(platformArn, arnResult);
	}

}
