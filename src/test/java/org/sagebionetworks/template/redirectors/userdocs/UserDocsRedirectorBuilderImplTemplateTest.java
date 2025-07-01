package org.sagebionetworks.template.redirectors.userdocs;


import org.apache.velocity.app.VelocityEngine;
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
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.Tag;

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
	private CloudFormationClientWrapper mockCloudFormationClientWrapper;

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
		builder = new UserDocsRedirectorBuilderImpl(mockConfig, mockCloudFormationClientWrapper, mockStackTagsProvider, velocityEngine);
	}

	@AfterEach
	void tearDown() {
	}

	@Test
	void testBuildUserDocsRedirectorStack() throws Exception {
		List<Tag> expectedTags = new ArrayList<>();
		Tag tag = Tag.builder().key("aKey").value("aValue").build();
		expectedTags.add(tag);
		Stack expectedStack = Stack.builder().stackName("tst-docs-synapse").tags(expectedTags).build();
		when(mockStackTagsProvider.getStackTags(mockConfig)).thenReturn(expectedTags);

		when(mockCloudFormationClientWrapper.waitForStackToComplete("tst-docs-synapse")).thenReturn(Optional.of(expectedStack));
		when(mockCloudFormationClientWrapper.describeStack("tst-docs-synapse")).thenReturn(Optional.of(expectedStack));

		// call under test
		Optional<Stack> optStack = builder.buildStack();

		assertTrue(optStack.isPresent());
		assertEquals("tst-docs-synapse", optStack.get().stackName());
		assertEquals(1, optStack.get().tags().size());
		assertEquals(tag, optStack.get().tags().get(0));

	}

}
