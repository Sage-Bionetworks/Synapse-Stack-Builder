package org.sagebionetworks.template.repo.beanstalk.image.encrypt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.sagebionetworks.template.repo.beanstalk.image.encrypt.ElasticBeanstalkDefaultAMIEncrypterImpl.AMI_VIRTUALIZATION_TYPE;
import static org.sagebionetworks.template.repo.beanstalk.image.encrypt.ElasticBeanstalkDefaultAMIEncrypterImpl.SOURCE_AMI_TAG_KEY;

import java.util.LinkedList;
import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CopyImageRequest;
import com.amazonaws.services.ec2.model.CopyImageResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.CustomAmi;
import com.amazonaws.services.elasticbeanstalk.model.DescribePlatformVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribePlatformVersionResult;
import com.amazonaws.services.elasticbeanstalk.model.ListPlatformVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.ListPlatformVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.PlatformDescription;
import com.amazonaws.services.elasticbeanstalk.model.PlatformSummary;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class ElasticBeanstalkDefaultAMIEncrypterImplTest {
	@Mock
	AWSElasticBeanstalkClient mockElasticBeanstalkClient;

	@Mock
	AmazonEC2Client mockEc2Client;

	@Mock
	LoggerFactory mockLogFactory;

	@Mock
	Logger mockLogger;

	ElasticBeanstalkDefaultAMIEncrypterImpl encrypter;

	final String originalImageId = "ami-originalId";
	final String copiedImageId = "newImageCopy";
	final String platformArn = "platformArn123";
	final String solutionStackName = "Java -42 with Tomcat 9000.1 version 1.2.3";

	PlatformDescription platformDescription;

	@Before
	public void setUp(){
		when(mockLogFactory.getLogger(ElasticBeanstalkDefaultAMIEncrypterImpl.class)).thenReturn(mockLogger);

		encrypter = new ElasticBeanstalkDefaultAMIEncrypterImpl(mockElasticBeanstalkClient, mockEc2Client, mockLogFactory);
		platformDescription = new PlatformDescription()
				.withSolutionStackName(solutionStackName)
				.withCustomAmiList(
						new CustomAmi()
								.withVirtualizationType("These are not the types you are looking for").withImageId("wrong1"),
						new CustomAmi()
								.withVirtualizationType(AMI_VIRTUALIZATION_TYPE).withImageId(originalImageId),
						new CustomAmi()
								.withVirtualizationType("Wrong again").withImageId("wrong2"));

		when(mockEc2Client.describeImages(any())).thenReturn(new DescribeImagesResult());
		when(mockEc2Client.copyImage(any()))
				.thenReturn(new CopyImageResult().withImageId(copiedImageId));
		when(mockElasticBeanstalkClient.listPlatformVersions(any(ListPlatformVersionsRequest.class)))
				.thenReturn(new ListPlatformVersionsResult().withPlatformSummaryList(new PlatformSummary().withPlatformArn(platformArn)));
	}


	@Test (expected = IllegalArgumentException.class)
	public void testGetEncryptedElasticBeanstalkAMI_nullJavaVersion(){
		encrypter.getEncryptedElasticBeanstalkAMI(null, "9000.1", "1.2.3");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetEncryptedElasticBeanstalkAMI_nullTomcatVersion(){
		encrypter.getEncryptedElasticBeanstalkAMI("-42", null, "1.2.3");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetEncryptedElasticBeanstalkAMI_nullAmazonLinuxVersion(){
		encrypter.getEncryptedElasticBeanstalkAMI("-42", "9000.1", null);
	}

	@Test
	public void testGetEncryptedElasticBeanstalkAMI(){
		when(mockElasticBeanstalkClient.describePlatformVersion(new DescribePlatformVersionRequest()
				.withPlatformArn(platformArn))).thenReturn(new DescribePlatformVersionResult().withPlatformDescription(platformDescription));

		ElasticBeanstalkEncryptedPlatformInfo expectedInfo = new ElasticBeanstalkEncryptedPlatformInfo(copiedImageId, solutionStackName);
		//method under test
		assertEquals(expectedInfo, encrypter.getEncryptedElasticBeanstalkAMI("-42", "9000.1", "1.2.3"));
	}


	@Test (expected = IllegalArgumentException.class)
	public void testFindDefaultPlatformAmi__noResults(){
		when(mockElasticBeanstalkClient.listPlatformVersions(any(ListPlatformVersionsRequest.class)))
				.thenReturn(new ListPlatformVersionsResult());

		//method under test
		encrypter.getPlatformArn("1", "2", "3");
	}

	@Test
	public void testFindDefaultPlatformAmi__resultFound(){
		//method under test
		String arnResult = encrypter.getPlatformArn("1", "2", "3");

		assertEquals(platformArn, arnResult);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testFindDefaultPlatformAmiId__noValuesFound(){
		PlatformDescription platformDescription = new PlatformDescription()
				.withCustomAmiList(new CustomAmi()
						.withVirtualizationType("These are not the types you are looking for"));
		//method under test
		encrypter.findDefaultPlatformAmiId(platformDescription);
	}

	@Test
	public void testFindDefaultPlatformAmiId_amiFound(){
		//method under test
		assertEquals(originalImageId, encrypter.findDefaultPlatformAmiId(platformDescription));
	}

	@Test
	public void testCopyAndEncryptAmiIfNecessary_needToCopy(){
		//method under test
		String result = encrypter.copyAndEncryptAmiIfNecessary(originalImageId);
		assertEquals(copiedImageId, result);

		CopyImageRequest expectedCopyRequest = new CopyImageRequest()
				.withEncrypted(true) //IMPORTANT!
				.withSourceImageId(originalImageId)
				.withSourceRegion("us-east-1");
		verify(mockEc2Client).copyImage(expectedCopyRequest);
		CreateTagsRequest expectedTagsRequest = new CreateTagsRequest()
				.withResources(copiedImageId)
				.withTags(new Tag().withKey(SOURCE_AMI_TAG_KEY).withValue(originalImageId));
		verify(mockEc2Client).createTags(expectedTagsRequest);

	}


	@Test
	public void testCopyAndEncryptAmiIfNecessary_copyAlreadyExists(){
		when(mockEc2Client.describeImages(any(DescribeImagesRequest.class))).thenReturn(
				new DescribeImagesResult().withImages(new Image().withImageId(copiedImageId)));

		//method under test
		String result = encrypter.copyAndEncryptAmiIfNecessary("ami-originalId");
		assertEquals(copiedImageId, result);

		//copy and tag should not be called
		verify(mockEc2Client, never()).copyImage(any());
		verify(mockEc2Client, never()).createTags(any());
	}

}
