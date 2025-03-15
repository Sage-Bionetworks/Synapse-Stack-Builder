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
import org.sagebionetworks.template.config.RepoConfiguration;

import software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient;
import software.amazon.awssdk.services.elasticbeanstalk.model.CustomAmi;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribePlatformVersionRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribePlatformVersionResponse;
import software.amazon.awssdk.services.elasticbeanstalk.model.ListPlatformVersionsRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.ListPlatformVersionsResponse;
import software.amazon.awssdk.services.elasticbeanstalk.model.PlatformDescription;
import software.amazon.awssdk.services.elasticbeanstalk.model.PlatformSummary;

@ExtendWith(MockitoExtension.class)
public class ElasticBeanstalkSolutionStackNameProviderImplTest {
	@Mock
	ElasticBeanstalkClient mockElasticBeanstalkClient;

	@Mock
	org.sagebionetworks.template.Ec2Client mockEc2Client;

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
		platformDescription = PlatformDescription.builder()
				.solutionStackName(solutionStackName)
				.customAmiList(
						CustomAmi.builder()
								.virtualizationType("These are not the types you are looking for").imageId("wrong1").build(),
						CustomAmi.builder()
								.virtualizationType(AMI_VIRTUALIZATION_TYPE).imageId(originalImageId).build(),
						CustomAmi.builder()
								.virtualizationType("Wrong again").imageId("wrong2").build())
				.build();
	}

	@Test
	public void testGetSolutionStackName(){
		when(mockElasticBeanstalkClient.listPlatformVersions(any(ListPlatformVersionsRequest.class)))
				.thenReturn(ListPlatformVersionsResponse.builder()
						.platformSummaryList(PlatformSummary.builder().platformArn(platformArn).build())
						.build());
		when(mockElasticBeanstalkClient.describePlatformVersion(DescribePlatformVersionRequest.builder()
				.platformArn(platformArn).build()))
				.thenReturn(DescribePlatformVersionResponse.builder()
						.platformDescription(platformDescription).build());;

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
				.thenReturn(ListPlatformVersionsResponse.builder().build());

		//method under test
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			encrypter.getPlatformArn("1", "2", "3");
		});
	}

	@Test
	public void testGetPlatformArn__resultFound(){
		when(mockElasticBeanstalkClient.listPlatformVersions(any(ListPlatformVersionsRequest.class)))
			.thenReturn(ListPlatformVersionsResponse.builder()
					.platformSummaryList(PlatformSummary.builder().platformArn(platformArn).build())
					.build());
		//method under test
		String arnResult = encrypter.getPlatformArn("1", "2", "3");

		assertEquals(platformArn, arnResult);
	}

}
