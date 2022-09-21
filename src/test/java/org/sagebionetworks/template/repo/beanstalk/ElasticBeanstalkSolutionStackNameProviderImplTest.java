package org.sagebionetworks.template.repo.beanstalk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.config.RepoConfiguration;

@RunWith(MockitoJUnitRunner.class)
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

	@Before
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

		when(mockElasticBeanstalkClient.listPlatformVersions(any(ListPlatformVersionsRequest.class)))
				.thenReturn(new ListPlatformVersionsResult().withPlatformSummaryList(new PlatformSummary().withPlatformArn(platformArn)));

	}

	@Test
	public void testGetEncryptedElasticBeanstalkAMI(){
		when(mockElasticBeanstalkClient.describePlatformVersion(new DescribePlatformVersionRequest()
				.withPlatformArn(platformArn))).thenReturn(new DescribePlatformVersionResult().withPlatformDescription(platformDescription));

		String expectedSolutionStackName = solutionStackName;
		//method under test
		assertEquals(expectedSolutionStackName, encrypter.getSolutionStackName("tomcatVersion", "javaVersion", "linuxVersion"));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetEncryptedElasticBeanstalkAMI_nullJavaVersion(){
		encrypter.getPlatformArn(null, "9000.1", "1.2.3");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetEncryptedElasticBeanstalkAMI_nullAmazonLinuxVersion(){
		encrypter.getPlatformArn("-42", "9000.1", null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetEncryptedElasticBeanstalkAMI_nullTomcatVersion(){
		encrypter.getPlatformArn("-42", null, "1.2.3");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetPlatformArn__noResults(){
		when(mockElasticBeanstalkClient.listPlatformVersions(any(ListPlatformVersionsRequest.class)))
				.thenReturn(new ListPlatformVersionsResult());

		//method under test
		encrypter.getPlatformArn("1", "2", "3");
	}

	@Test
	public void testGetPlatformArn__resultFound(){
		//method under test
		String arnResult = encrypter.getPlatformArn("1", "2", "3");

		assertEquals(platformArn, arnResult);
	}

}
