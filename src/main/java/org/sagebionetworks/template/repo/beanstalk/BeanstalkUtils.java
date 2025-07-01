package org.sagebionetworks.template.repo.beanstalk;

import software.amazon.awssdk.services.elasticbeanstalk.model.ListPlatformVersionsRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.PlatformFilter;
import software.amazon.awssdk.services.elasticbeanstalk.model.PlatformSummary;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class BeanstalkUtils {

	static final String PLATFORM_NAME_TEMPLATE =  "Tomcat %s with Corretto %s running on 64bit Amazon Linux 2023";

	public static ListPlatformVersionsRequest buildListPlatformVersionsRequest(String javaVersion, String tomcatVersion, String amazonLinuxVersion) {
		if(javaVersion == null){
			throw new IllegalArgumentException("javaVersion cannot be null");
		}
		if(tomcatVersion == null){
			throw new IllegalArgumentException("tomcatVersion cannot be null");
		}

		//filters to be used for finding platform arn
		Collection<PlatformFilter> filters = new LinkedList<>();

		PlatformFilter filter = PlatformFilter.builder()
				.type("PlatformName")
				.operator("=")
				.values(String.format(PLATFORM_NAME_TEMPLATE, tomcatVersion, javaVersion))
				.build();
		filters.add(filter);

		if (amazonLinuxVersion != null) {
			filter = PlatformFilter.builder()
					.type("PlatformVersion")
					.operator("=")
					.values(amazonLinuxVersion)
					.build();
			filters.add(filter);
		}

		ListPlatformVersionsRequest request = ListPlatformVersionsRequest.builder().filters(filters).build();

		return request;

	}

	public static String getLatestPlatformVersion(List<PlatformSummary> summaries) {
		if (summaries == null || summaries.size() == 0) {
			throw new IllegalArgumentException("Argument 'summaries' cannot be null or empty");
		}
		Comparator<PlatformSummary> comparator = Comparator.comparing(PlatformSummary::platformVersion);
		String maxPlatformVersion = summaries.stream().max(comparator).get().platformVersion();
		return maxPlatformVersion;
	}

}
