package org.sagebionetworks.template.cdn;

import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.Tag;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.TemplateUtils;
import org.sagebionetworks.template.config.RepoConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CdnBuilderImplTemplateTest {
	@Mock
	private RepoConfiguration mockConfig;

	@Mock
	private CloudFormationClient mockCloudFormationClient;

	@Mock
	private StackTagsProvider mockStackTagsProvider;

	@Captor
	private ArgumentCaptor<CreateOrUpdateStackRequest> createOrUpdateStackRequestArgumentCaptor;


	VelocityEngine velocityEngine;
	CdnBuilderImpl builder;

	@BeforeEach
	void setUp() {
		when(mockConfig.getProperty("org.sagebionetworks.beanstalk.ssl.arn.portal")).thenReturn("acmarn");
		when(mockConfig.getProperty("org.sagebionetworks.stack.instance.alias")).thenReturn("dev");
		when(mockConfig.getProperty("org.sagebionetworks.stack")).thenReturn("tst");
		when(mockConfig.getProperty("org.sagebionetworks.cloudfront.public.key.encoded")).thenReturn("1234");
		when(mockConfig.getProperty("org.sagebionetworks.cloudfront.certificate.arn")).thenReturn("arn:aws:acm:us-east-1:5678:certificate/1234");
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();
		builder = new CdnBuilderImpl(mockConfig, mockCloudFormationClient, mockStackTagsProvider, velocityEngine);
	}

	@AfterEach
	void tearDown() {
	}

	@Test
	void testBuildCdnStack() throws Exception {
		List<Tag> expectedTags = new ArrayList<>();
		Tag tag = new Tag().withKey("aKey").withValue("aValue");
		expectedTags.add(tag);
		Stack expectedStack = new Stack().withStackName("cdn-dev-synapse").withTags(expectedTags);
		when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);

		when(mockCloudFormationClient.waitForStackToComplete(any(String.class))).thenReturn(Optional.of(expectedStack));
		when(mockCloudFormationClient.describeStack(any(String.class))).thenReturn(Optional.of(expectedStack));

		// call under test
		Optional<Stack> optStack = builder.buildCdnStack(CdnBuilder.Type.PORTAL);

		assertTrue(optStack.isPresent());
		assertEquals("cdn-dev-synapse", optStack.get().getStackName());
		assertEquals(1, optStack.get().getTags().size());
		assertEquals(tag, optStack.get().getTags().get(0));

	}

	@Test
	void testBuildDataCdnStackTemplate() throws Exception{
		List<Tag> expectedTags = new ArrayList<>();
		Tag tag = new Tag().withKey("aKey").withValue("aValue");
		expectedTags.add(tag);
		Stack expectedStack = new Stack().withStackName("cdn-tst-data-synapse").withTags(expectedTags);
		when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);

		when(mockCloudFormationClient.waitForStackToComplete(any(String.class))).thenReturn(Optional.of(expectedStack));
		when(mockCloudFormationClient.describeStack(any(String.class))).thenReturn(Optional.of(expectedStack));

		// call under test
		Optional<Stack> optStack = builder.buildCdnStack(CdnBuilder.Type.DATA);

		String expectedDataCdnTemplate = new JSONObject(TemplateUtils.loadContentFromFile("cdn/synapse-data-cdn-test.json")).toString(5);

		verify(mockCloudFormationClient).createOrUpdateStack(createOrUpdateStackRequestArgumentCaptor.capture());
		CreateOrUpdateStackRequest req = createOrUpdateStackRequestArgumentCaptor.getValue();
		assertEquals("cdn-tst-data-synapse", req.getStackName());
		assertEquals(expectedDataCdnTemplate, new JSONObject(req.getTemplateBody()).toString(5));
		assertEquals(expectedTags, req.getTags());

		assertTrue(optStack.isPresent());
		assertEquals("cdn-tst-data-synapse", optStack.get().getStackName());
		assertEquals(1, optStack.get().getTags().size());
		assertEquals(tag, optStack.get().getTags().get(0));
	}
}
