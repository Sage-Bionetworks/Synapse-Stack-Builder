package org.sagebionetworks.template.cdn;

import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.Tag;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.RepoConfiguration;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
		when(mockConfig.getProperty("org.sagebionetworks.stack.instance.alias")).thenReturn("tst");
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
		Stack expectedStack = new Stack().withStackName("cdn-tst-synapse").withTags(expectedTags);
		when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);

		when(mockCloudFormationClient.waitForStackToComplete(any(String.class))).thenReturn(Optional.of(expectedStack));
		when(mockCloudFormationClient.describeStack(any(String.class))).thenReturn(Optional.of(expectedStack));

		// call under test
		Optional<Stack> optStack = builder.buildCdnStack(CdnBuilder.Type.PORTAL);

		assertTrue(optStack.isPresent());
		assertEquals("cdn-tst-synapse", optStack.get().getStackName());
		assertEquals(1, optStack.get().getTags().size());
		assertEquals(tag, optStack.get().getTags().get(0));

	}
}
