package org.sagebionetworks.template.repo.beanstalk;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.elasticbeanstalk.model.ListPlatformVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.PlatformFilter;
import com.amazonaws.services.elasticbeanstalk.model.PlatformSummary;
import org.junit.jupiter.api.Test;

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
		List<PlatformFilter> filters = req.getFilters();
		assertNotNull(filters);
		// Should be 2 filters
		assertEquals(2, filters.size());
		PlatformFilter f1 = filters.get(0);
		assertEquals("PlatformName", f1.getType());
		assertEquals("Tomcat 8.5 with Java 11 running on 64bit Amazon Linux", f1.getValues().get(0));
		assertEquals("=", f1.getOperator());
		PlatformFilter f2 = filters.get(1);
		assertEquals("PlatformVersion", f2.getType());
		assertEquals("3.4.6", f2.getValues().get(0));
		assertEquals("=", f2.getOperator());

	}

	@Test
	void buildListPlatformVersionsRequestNoVersion() {

		// call under test
		ListPlatformVersionsRequest req = BeanstalkUtils.buildListPlatformVersionsRequest("11", "8.5", null);
		assertNotNull(req);
		List<PlatformFilter> filters = req.getFilters();
		assertNotNull(filters);
		// Should be 2 filters
		assertEquals(1, filters.size());
		PlatformFilter f1 = filters.get(0);
		assertEquals("PlatformName", f1.getType());
		assertEquals("Tomcat 8.5 with Java 11 running on 64bit Amazon Linux", f1.getValues().get(0));
		assertEquals("=", f1.getOperator());

	}

	@Test
	void getLatestPlatformVersion() {

		PlatformSummary s1 = new PlatformSummary().withPlatformVersion("1.2.3");
		PlatformSummary s2 = new PlatformSummary().withPlatformVersion("4.5.6");
		PlatformSummary s3 = new PlatformSummary().withPlatformVersion("2.3.4");
		List<PlatformSummary> summaries = Arrays.asList(s1, s2, s3);

		String latestVersion =  BeanstalkUtils.getLatestPlatformVersion(summaries);
		assertEquals("4.5.6", latestVersion);

	}

}