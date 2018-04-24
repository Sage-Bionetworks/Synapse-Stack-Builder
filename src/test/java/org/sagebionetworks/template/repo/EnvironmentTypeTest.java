package org.sagebionetworks.template.repo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

public class EnvironmentTypeTest {

	@Test
	public void testCreateArtifactoryUrlRepo() {
		String result = EnvironmentType.REPOSITORY_SERVICES.createArtifactoryUrl("222.0");
		assertEquals(
				"http://sagebionetworks.artifactoryonline.com/sagebionetworks"
				+ "/libs-releases-local/org/sagebionetworks"
				+ "/services-repository/222.0/services-repository-222.0.war",
				result);
	}
	
	@Test
	public void testCreateArtifactoryUrlWorkers() {
		String result = EnvironmentType.REPOSITORY_WORKERS.createArtifactoryUrl("222.0");
		assertEquals(
				"http://sagebionetworks.artifactoryonline.com/sagebionetworks"
				+ "/libs-releases-local/org/sagebionetworks"
				+ "/services-workers/222.0/services-workers-222.0.war",
				result);
	}

	
	@Test
	public void testCreateArtifactoryUrlPortal() {
		String result = EnvironmentType.PORTAL.createArtifactoryUrl("222.0");
		assertEquals(
				"http://sagebionetworks.artifactoryonline.com/sagebionetworks"
				+ "/libs-releases-local/org/sagebionetworks"
				+ "/portal/222.0/portal-222.0.war",
				result);
	}
	
	@Test
	public void testCreateS3KeyRepo() {
		String result = EnvironmentType.REPOSITORY_SERVICES.createS3Key("222.0");
		assertEquals("versions/services-repository/services-repository-222.0.war", result);
	}
	
	@Test
	public void testCreateS3KeyWorkers() {
		String result = EnvironmentType.REPOSITORY_WORKERS.createS3Key("222.0");
		assertEquals("versions/services-workers/services-workers-222.0.war", result);
	}
	
	@Test
	public void testCreateS3KeyPortal() {
		String result = EnvironmentType.PORTAL.createS3Key("222.0");
		assertEquals("versions/portal/portal-222.0.war", result);
	}
}
