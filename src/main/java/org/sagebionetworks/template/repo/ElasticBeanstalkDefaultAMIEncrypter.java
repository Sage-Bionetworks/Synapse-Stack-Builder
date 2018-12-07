package org.sagebionetworks.template.repo;

import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CopyImageRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.CustomAmi;
import com.amazonaws.services.elasticbeanstalk.model.DescribePlatformVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribePlatformVersionResult;
import com.amazonaws.services.elasticbeanstalk.model.ListPlatformVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.ListPlatformVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.PlatformDescription;
import com.amazonaws.services.elasticbeanstalk.model.PlatformFilter;
import com.amazonaws.services.elasticbeanstalk.model.PlatformSummary;
import com.google.inject.Inject;

public class ElasticBeanstalkDefaultAMIEncrypter {
	AWSElasticBeanstalk elasticBeanstalk;
	AmazonEC2 ec2;

	private static final String PLATFORM_NAME_TEMPLATE =  "Tomcat %s with Java %s running on 64bit Amazon Linux";
	private static final String AMI_VIRTUALIZATION_TYPE = "hvm";

	@Inject
	public ElasticBeanstalkDefaultAMIEncrypter(AWSElasticBeanstalk elasticBeanstalk, AmazonEC2 ec2) {
		this.elasticBeanstalk = elasticBeanstalk;
		this.ec2 = ec2;
	}

	/**
	 *
	 * @param javaVersion
	 * @param tomcatVersion
	 * @param amazonLinuxVersion
	 * @return AMI Id of the encrypted version of the default AWS AMI
	 */
	public String getEncryptedElasticBeanstalkDefaultAMI(String javaVersion, String tomcatVersion, String amazonLinuxVersion){
		if(javaVersion == null){
			throw new IllegalArgumentException("javaVersion cannot be null");
		}

		if(tomcatVersion == null){
			throw new IllegalArgumentException("tomcatVersion cannot be null");
		}

		if (amazonLinuxVersion == null){
			amazonLinuxVersion = "latest";
		}

		//find the ARN of the platform from passed in parameters
		String platformArn= getPlatformArn(javaVersion, tomcatVersion, amazonLinuxVersion);

		//use the platformArn to retrieve the platform's AMI image id
		DescribePlatformVersionResult describePlatformResult = elasticBeanstalk.describePlatformVersion(new DescribePlatformVersionRequest().withPlatformArn(platformArn));
		PlatformDescription description = describePlatformResult.getPlatformDescription();

		String solutionStackName = description.getSolutionStackName();
		String defaultAMI = null;
		for (CustomAmi customAmi : description.getCustomAmiList()){
			if(AMI_VIRTUALIZATION_TYPE.equals(customAmi.getVirtualizationType())){
				defaultAMI = customAmi.getImageId();
			}
		}
		if(defaultAMI == null) {
			throw new IllegalArgumentException("Could not find an AMI Image Id for the given parameters");
		}

		//check if we've already copied and encrypted the image:
		DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest().withOwners("self").withFilters(new Filter().withName());


		//copy the image
		ec2.copyImage(CopyImageRequest co)

	}

	private String getPlatformArn(String javaVersion, String tomcatVersion, String amazonLinuxVersion) {
		PlatformFilter tomcatJavaFilter = new PlatformFilter().withType("PlatformName").withOperator("=")
				.withValues(String.format(PLATFORM_NAME_TEMPLATE, tomcatVersion, javaVersion));
		PlatformFilter amazonLinuxFilter = new PlatformFilter().withType("PlatformVersion").withOperator("=")
				.withValues(amazonLinuxVersion);
		ListPlatformVersionsRequest listRequest = new ListPlatformVersionsRequest().withFilters(tomcatJavaFilter, amazonLinuxFilter);
		ListPlatformVersionsResult listResult = elasticBeanstalk.listPlatformVersions(listRequest);
		List<PlatformSummary> platformSummaryList = listResult.getPlatformSummaryList();
		if(platformSummaryList == null || platformSummaryList.size() != 1){
			throw new IllegalArgumentException("There should only be 1 result matching your parameters");
		}
		return platformSummaryList.get(0).getPlatformArn();
	}
}
