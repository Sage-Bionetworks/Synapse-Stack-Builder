package org.sagebionetworks.template.repo.beanstalk;

import com.amazonaws.services.elasticbeanstalk.model.ListPlatformVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.PlatformFilter;
import com.amazonaws.services.elasticbeanstalk.model.PlatformSummary;
import org.sagebionetworks.template.Constants;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class BeanstalkUtils {

	static final String PLATFORM_NAME_TEMPLATE =  "Tomcat %s with Java %s running on 64bit Amazon Linux";

	public static ListPlatformVersionsRequest buildListPlatformVersionsRequest(String javaVersion, String tomcatVersion, String amazonLinuxVersion) {
		if(javaVersion == null){
			throw new IllegalArgumentException("javaVersion cannot be null");
		}
		if(tomcatVersion == null){
			throw new IllegalArgumentException("tomcatVersion cannot be null");
		}

		//filters to be used for finding platform arn
		Collection<PlatformFilter> filters = new LinkedList<>();

		PlatformFilter filter = new PlatformFilter()
				.withType("PlatformName")
				.withOperator("=")
				.withValues(String.format(PLATFORM_NAME_TEMPLATE, tomcatVersion, javaVersion));
		filters.add(filter);

		if (amazonLinuxVersion != null) {
			filter = new PlatformFilter()
					.withType("PlatformVersion")
					.withOperator("=")
					.withValues(amazonLinuxVersion);
			filters.add(filter);
		}

		ListPlatformVersionsRequest request = new ListPlatformVersionsRequest().withFilters(filters);

		return request;

	}

	public static String getLatestPlatformVersion(List<PlatformSummary> summaries) {
		if (summaries == null || summaries.size() == 0) {
			throw new IllegalArgumentException("Argument 'summaries' cannot be null or empty");
		}
		Comparator<PlatformSummary> comparator = Comparator.comparing(PlatformSummary::getPlatformVersion);
		String maxPlatformVersion = summaries.stream().max(comparator).get().getPlatformVersion();
		return maxPlatformVersion;
	}

}
