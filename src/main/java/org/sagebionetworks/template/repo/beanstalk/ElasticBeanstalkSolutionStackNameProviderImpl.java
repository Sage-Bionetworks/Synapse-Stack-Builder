package org.sagebionetworks.template.repo.beanstalk;

import java.util.List;

import com.google.inject.Inject;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.config.RepoConfiguration;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribePlatformVersionRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.PlatformDescription;
import software.amazon.awssdk.services.elasticbeanstalk.model.PlatformSummary;

public class ElasticBeanstalkSolutionStackNameProviderImpl implements ElasticBeanstalkSolutionStackNameProvider {
	ElasticBeanstalkClient elasticBeanstalk;
	Ec2Client ec2;
	Configuration config;

	static final String AMI_VIRTUALIZATION_TYPE = "hvm";
	static final String SOURCE_AMI_TAG_KEY = "CopiedFrom";

	@Inject
	public ElasticBeanstalkSolutionStackNameProviderImpl(ElasticBeanstalkClient elasticBeanstalk, Ec2Client ec2, RepoConfiguration config) {
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
		DescribePlatformVersionRequest req = DescribePlatformVersionRequest.builder().platformArn(platformArn).build();
		PlatformDescription description = elasticBeanstalk.describePlatformVersion(req).platformDescription();
		String solutionStackName = description.solutionStackName();

		return solutionStackName;
	}

	String getPlatformArn(String javaVersion, String tomcatVersion, String amazonLinuxVersion) {
		// This can be null in buildListPlatformVersionsRequest so check here
		if(amazonLinuxVersion == null){
			throw new IllegalArgumentException("amazonLinuxVersion cannot be null");
		}
		List<PlatformSummary> platformSummaryList = elasticBeanstalk.listPlatformVersions(
				BeanstalkUtils.buildListPlatformVersionsRequest(javaVersion, tomcatVersion, amazonLinuxVersion)
		).platformSummaryList();

		if(platformSummaryList == null || platformSummaryList.size() != 1){
			throw new IllegalArgumentException("There should only be 1 result matching your elastic beanstalk platform parameters");
		}

		return platformSummaryList.get(0).platformArn();
	}

}
