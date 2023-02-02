package org.sagebionetworks.template.redirectors.userdocs;

import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.Tag;
import org.apache.velocity.app.VelocityEngine;
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
import org.sagebionetworks.template.config.RepoConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserDocsRedirectorBuilderImplTemplateTest {

	@Mock
	private RepoConfiguration mockConfig;

	@Mock
	private CloudFormationClient mockCloudFormationClient;

	@Mock
	private StackTagsProvider mockStackTagsProvider;

	@Captor
	private ArgumentCaptor<CreateOrUpdateStackRequest> createOrUpdateStackRequestArgumentCaptor;

	VelocityEngine velocityEngine;
	UserDocsRedirectorBuilderImpl builder;

	@BeforeEach
	void setUp() {
		when(mockConfig.getProperty("org.sagebionetworks.beanstalk.ssl.arn.portal")).thenReturn("acmarn");
		when(mockConfig.getProperty("org.sagebionetworks.stack.instance.alias")).thenReturn("tst");
		velocityEngine = new TemplateGuiceModule().velocityEngineProvider();
		builder = new UserDocsRedirectorBuilderImpl(mockConfig, mockCloudFormationClient, mockStackTagsProvider, velocityEngine);
	}

	@AfterEach
	void tearDown() {
	}

	@Test
	void testBuildUserDocsRedirectorStack() throws Exception {
		List<Tag> expectedTags = new ArrayList<>();
		Tag tag = new Tag().withKey("aKey").withValue("aValue");
		expectedTags.add(tag);
		Stack expectedStack = new Stack().withStackName("tst-docs-synapse").withTags(expectedTags);
		when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);

		when(mockCloudFormationClient.waitForStackToComplete("tst-docs-synapse")).thenReturn(Optional.of(expectedStack));
		when(mockCloudFormationClient.describeStack("tst-docs-synapse")).thenReturn(Optional.of(expectedStack));

		// call under test
		Optional<Stack> optStack = builder.buildStack();

		assertTrue(optStack.isPresent());
		assertEquals("tst-docs-synapse", optStack.get().getStackName());
		assertEquals(1, optStack.get().getTags().size());
		assertEquals(tag, optStack.get().getTags().get(0));

	}

}
