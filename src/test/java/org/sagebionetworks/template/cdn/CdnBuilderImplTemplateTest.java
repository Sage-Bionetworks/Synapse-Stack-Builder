package org.sagebionetworks.template.cdn;

import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.Tag;
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
import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.TemplateUtils;

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
	private CloudFormationClientWrapper mockCloudFormationClientWrapper;

	@Mock
	private StackTagsProvider mockStackTagsProvider;

	@Captor
	private ArgumentCaptor<CreateOrUpdateStackRequest> createOrUpdateStackRequestArgumentCaptor;


	VelocityEngine velocityEngine;
	CdnBuilderImpl builder;

	private static final String FAKE_PUBLIC_KEY = "1234";

	@BeforeEach
	void setUp() {
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();
		builder = new CdnBuilderImpl(mockConfig, mockCloudFormationClientWrapper, mockStackTagsProvider, velocityEngine);
	}

	@AfterEach
	void tearDown() {
	}

	@Test
	void testBuildCdnStack() throws Exception {
		List<Tag> expectedTags = new ArrayList<>();
		Tag tag = Tag.builder().key("aKey").value("aValue").build();
		expectedTags.add(tag);
		Stack expectedStack = Stack.builder().stackName("cdn-tst-synapse").tags(expectedTags).build();
		when(mockStackTagsProvider.getStackTags(mockConfig)).thenReturn(expectedTags);

		when(mockCloudFormationClientWrapper.waitForStackToComplete(any(String.class))).thenReturn(Optional.of(expectedStack));
		when(mockCloudFormationClientWrapper.describeStack(any(String.class))).thenReturn(Optional.of(expectedStack));

		when(mockConfig.getProperty("org.sagebionetworks.beanstalk.ssl.arn.portal")).thenReturn("acmarn");
		when(mockConfig.getProperty("org.sagebionetworks.stack.instance.alias")).thenReturn("tst");
		when(mockConfig.getProperty("org.sagebionetworks.stack")).thenReturn("dev");

		// call under test
		Optional<Stack> optStack = builder.buildCdnStack(CdnBuilder.Type.PORTAL);

		assertTrue(optStack.isPresent());
		assertEquals("cdn-tst-synapse", optStack.get().stackName());
		assertEquals(1, optStack.get().tags().size());
		assertEquals(tag, optStack.get().tags().get(0));

	}

	@Test
	void testBuildCdnStackProd() throws Exception {
		List<Tag> expectedTags = new ArrayList<>();
		Tag tag = Tag.builder().key("aKey").value("aValue").build();
		expectedTags.add(tag);
		Stack expectedStack = Stack.builder().stackName("cdn-prod-synapse").tags(expectedTags).build();
		when(mockStackTagsProvider.getStackTags(mockConfig)).thenReturn(expectedTags);

		when(mockCloudFormationClientWrapper.waitForStackToComplete(any(String.class))).thenReturn(Optional.of(expectedStack));
		when(mockCloudFormationClientWrapper.describeStack(any(String.class))).thenReturn(Optional.of(expectedStack));

		when(mockConfig.getProperty("org.sagebionetworks.beanstalk.ssl.arn.portal")).thenReturn("acmarn");
		when(mockConfig.getProperty("org.sagebionetworks.stack.instance.alias")).thenReturn("prod");
		when(mockConfig.getProperty("org.sagebionetworks.stack")).thenReturn("dev");

		// call under test
		Optional<Stack> optStack = builder.buildCdnStack(CdnBuilder.Type.PORTAL);

		assertTrue(optStack.isPresent());
		assertEquals("cdn-prod-synapse", optStack.get().stackName());
		assertEquals(1, optStack.get().tags().size());
		assertEquals(tag, optStack.get().tags().get(0));

	}

	@Test
	void testBuildDataCdnStackTemplate() throws Exception{
		List<Tag> expectedTags = new ArrayList<>();
		Tag tag = Tag.builder().key("aKey").value("aValue").build();
		expectedTags.add(tag);
		Stack expectedStack = Stack.builder().stackName("cdn-tst-data-synapse").tags(expectedTags).build();
		when(mockStackTagsProvider.getStackTags(mockConfig)).thenReturn(expectedTags);

		when(mockCloudFormationClientWrapper.waitForStackToComplete(any(String.class))).thenReturn(Optional.of(expectedStack));
		when(mockCloudFormationClientWrapper.describeStack(any(String.class))).thenReturn(Optional.of(expectedStack));

		when(mockConfig.getProperty("org.sagebionetworks.stack")).thenReturn("tst");
		when(mockConfig.getProperty("org.sagebionetworks.cloudfront.public.key.encoded")).thenReturn(FAKE_PUBLIC_KEY);
		when(mockConfig.getProperty("org.sagebionetworks.cloudfront.certificate.arn")).thenReturn("arn:aws:acm:us-east-1:5678:certificate/1234");

		// call under test
		Optional<Stack> optStack = builder.buildCdnStack(CdnBuilder.Type.DATA);

		String expectedDataCdnTemplate = new JSONObject(TemplateUtils.loadContentFromFile("cdn/synapse-data-cdn-test.json")).toString(5);

		verify(mockCloudFormationClientWrapper).createOrUpdateStack(createOrUpdateStackRequestArgumentCaptor.capture());
		CreateOrUpdateStackRequest req = createOrUpdateStackRequestArgumentCaptor.getValue();
		assertEquals("cdn-tst-data-synapse", req.getStackName());
		assertEquals(expectedDataCdnTemplate, new JSONObject(req.getTemplateBody()).toString(5));
		assertEquals(expectedTags, req.getTags());

		assertTrue(optStack.isPresent());
		assertEquals("cdn-tst-data-synapse", optStack.get().stackName());
		assertEquals(1, optStack.get().tags().size());
		assertEquals(tag, optStack.get().tags().get(0));
	}
}
