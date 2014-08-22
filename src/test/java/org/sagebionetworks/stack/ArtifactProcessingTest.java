package org.sagebionetworks.stack;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.sagebionetworks.factory.MockAmazonClientFactory;
import org.sagebionetworks.stack.config.InputConfiguration;

public class ArtifactProcessingTest {
	
//	InputConfiguration config;	
//	GeneratedResources resources;
//
//	MockAmazonClientFactory factory = new MockAmazonClientFactory();
//	AWSElasticBeanstalkClient mockClient;
//	ElasticBeanstalkSetup setup;
//	
//	@Before
//	public void before() throws IOException {
//		mockClient = factory.createBeanstalkClient();
//		config = TestHelper.createTestConfig("stack");
//		resources = TestHelper.createTestResources(config);
//		setup = new ElasticBeanstalkSetup(factory, config, resources);
//	}
	
	@Test
	public void testIsArtifactoryError404Response() throws IOException {
		String resp = "{\n  \"errors\" : [ {\n    \"status\" : 404,\n    \"message\" : \"File not found.\"\n  } ]}";
		boolean rc = ArtifactProcessing.isArtifactoryError404Response(resp);
		assertTrue(rc);
		resp = "{\n  \"errors\" : [ {\n    \"status\" : 403,\n    \"message\" : \"File not found.\"\n  } ]}";
		rc = ArtifactProcessing.isArtifactoryError404Response(resp);
		assertFalse(rc);
		resp = "{\n  \"errors\" : [ {\n    \"status\" : 403,\n  } ]}";
		rc = ArtifactProcessing.isArtifactoryError404Response(resp);
		assertFalse(rc);
		resp = "not JSON";
		rc = ArtifactProcessing.isArtifactoryError404Response(resp);
		assertFalse(rc);
	}
}
