package org.sagebionetworks.template.repo.beanstalk.image.encrypt;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ElasticBeanstalkDefaultAMIEncrypterImplTest {
	@Mock
	AWSElasticBeanstalkClient mockElasticBeanstalkClient;

	@Mock
	AmazonEC2Client mockEc2Client;

	@Mock
	LogFactory mockLogFactory;

	@Test
	public void test(){
		
	}

}
