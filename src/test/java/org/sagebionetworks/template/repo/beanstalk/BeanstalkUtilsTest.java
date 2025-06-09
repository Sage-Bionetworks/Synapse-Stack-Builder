package org.sagebionetworks.template.repo.beanstalk;

import com.amazonaws.services.ec2.model.Filter;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.elasticbeanstalk.model.ListPlatformVersionsRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.PlatformFilter;
import software.amazon.awssdk.services.elasticbeanstalk.model.PlatformSummary;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BeanstalkUtilsTest {

	@Test
	void buildListPlatformVersionsRequest() {

		// call under test
		ListPlatformVersionsRequest req = BeanstalkUtils.buildListPlatformVersionsRequest("11", "8.5", "3.4.6");
		assertNotNull(req);
		List<PlatformFilter> filters = req.filters();
		assertNotNull(filters);
		// Should be 2 filters
		assertEquals(2, filters.size());
		PlatformFilter f1 = filters.get(0);
		assertEquals("PlatformName", f1.type());
		assertEquals("Tomcat 8.5 with Corretto 11 running on 64bit Amazon Linux 2023", f1.values().get(0));
		assertEquals("=", f1.operator());
		PlatformFilter f2 = filters.get(1);
		assertEquals("PlatformVersion", f2.type());
		assertEquals("3.4.6", f2.values().get(0));
		assertEquals("=", f2.operator());

	}

	@Test
	void buildListPlatformVersionsRequestNoVersion() {

		// call under test
		ListPlatformVersionsRequest req = BeanstalkUtils.buildListPlatformVersionsRequest("11", "8.5", null);
		assertNotNull(req);
		List<PlatformFilter> filters = req.filters();
		assertNotNull(filters);
		// Should be 2 filters
		assertEquals(1, filters.size());
		PlatformFilter f1 = filters.get(0);
		assertEquals("PlatformName", f1.type());
		assertEquals("Tomcat 8.5 with Corretto 11 running on 64bit Amazon Linux 2023", f1.values().get(0));
		assertEquals("=", f1.operator());

	}

	@Test
	void getLatestPlatformVersion() {

		PlatformSummary s1 = PlatformSummary.builder().platformVersion("1.2.3").build();
		PlatformSummary s2 = PlatformSummary.builder().platformVersion("4.5.6").build();
		PlatformSummary s3 = PlatformSummary.builder().platformVersion("2.3.4").build();
		List<PlatformSummary> summaries = Arrays.asList(s1, s2, s3);

		String latestVersion =  BeanstalkUtils.getLatestPlatformVersion(summaries);
		assertEquals("4.5.6", latestVersion);

	}

}