package org.sagebionetworks.template.repo.beanstalk;

import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.DescribePlatformVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.PlatformDescription;
import com.amazonaws.services.elasticbeanstalk.model.PlatformSummary;
import com.google.inject.Inject;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.config.RepoConfiguration;

public class ElasticBeanstalkSolutionStackNameProviderImpl implements ElasticBeanstalkSolutionStackNameProvider {
	AWSElasticBeanstalk elasticBeanstalk;
	AmazonEC2 ec2;
	Configuration config;

	static final String AMI_VIRTUALIZATION_TYPE = "hvm";
	static final String SOURCE_AMI_TAG_KEY = "CopiedFrom";

	@Inject
	public ElasticBeanstalkSolutionStackNameProviderImpl(AWSElasticBeanstalk elasticBeanstalk, AmazonEC2 ec2, RepoConfiguration config) {
		this.elasticBeanstalk = elasticBeanstalk;
		this.ec2 = ec2;
		this.config = config;
	}

	@Override
	public String getSolutionStackName(String tomcatVersion, String javaVersion, String linuxVersion){
		//find the ARN of the platform from versions in the config
		String platformArn= getPlatformArn(
				javaVersion,
				tomcatVersion,
				linuxVersion);

		//use the platformArn to retrieve the platform's AMI image id
		PlatformDescription description = elasticBeanstalk.describePlatformVersion(
				new DescribePlatformVersionRequest().withPlatformArn(platformArn)
			).getPlatformDescription();

		String solutionStackName = description.getSolutionStackName();
		return solutionStackName;
	}

	String getPlatformArn(String javaVersion, String tomcatVersion, String amazonLinuxVersion) {
		// This can be null in buildListPlatformVersionsRequest so check here
		if(amazonLinuxVersion == null){
			throw new IllegalArgumentException("amazonLinuxVersion cannot be null");
		}
		List<PlatformSummary> platformSummaryList = elasticBeanstalk.listPlatformVersions(
				BeanstalkUtils.buildListPlatformVersionsRequest(javaVersion, tomcatVersion, amazonLinuxVersion)
		).getPlatformSummaryList();

		if(platformSummaryList == null || platformSummaryList.size() != 1){
			throw new IllegalArgumentException("There should only be 1 result matching your elastic beanstalk platform parameters");
		}

		return platformSummaryList.get(0).getPlatformArn();
	}

}
