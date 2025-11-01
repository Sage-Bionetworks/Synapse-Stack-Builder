package org.sagebionetworks.template.redirectors.userdocs;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
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
import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.RepoConfiguration;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.Tag;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDocsRedirectorBuilderImplTest {

	@Mock
	private RepoConfiguration mockConfig;

	@Mock
	private CloudFormationClientWrapper mockCloudFormationClientWrapper;

	@Mock
	private StackTagsProvider mockStackTagsProvider;

	@Mock
	private VelocityEngine mockVelocityEngine;

	@Mock
	private Template mockTemplate;

	@Captor
	private ArgumentCaptor<CreateOrUpdateStackRequest> createOrUpdateStackRequestArgumentCaptor;

	@InjectMocks
	UserDocsRedirectorBuilderImpl builder;

	@BeforeEach
	void setUp() {
		when(mockConfig.getProperty("org.sagebionetworks.docs.ssl.arn")).thenReturn("acmarn");
		when(mockConfig.getProperty("org.sagebionetworks.stack.instance.alias")).thenReturn("tst");
	}

	@AfterEach
	void tearDown() {
	}

	@Test
	void testCreateContext() {

		// call under test
		VelocityContext ctxt = builder.createContext();

		verify(mockConfig).getProperty("org.sagebionetworks.docs.ssl.arn");
		verify(mockConfig).getProperty("org.sagebionetworks.stack.instance.alias");
		assertEquals("acmarn", ctxt.get("AcmCertificateArn"));
		assertEquals("tst", ctxt.get("SubDomainName"));
		assertEquals("docs.synapse.org", ctxt.get("DomainName"));
	}

	@Test
	void testBuildStack() throws Exception {
		when(mockVelocityEngine.getTemplate(any())).thenReturn(mockTemplate);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				StringWriter writer = (StringWriter) invocation.getArgument(1);
				writer.append("someYamlTemplate");
				return null;
			}
		}).when(mockTemplate).merge(any(), any());
		List<Tag> expectedTags = new ArrayList<>();
		Tag tag = Tag.builder().key("aKey").value("aValue").build();
		expectedTags.add(tag);
		Stack expectedStack = Stack.builder().stackName("tst-docs-synapse").tags(expectedTags).build();
		when(mockStackTagsProvider.getStackTags(mockConfig)).thenReturn(expectedTags);

		when(mockCloudFormationClientWrapper.waitForStackToComplete(any())).thenReturn(Optional.of(expectedStack));
		when(mockCloudFormationClientWrapper.describeStack(any())).thenReturn(Optional.of(expectedStack));

		// call under test
		Optional<Stack> optStack = builder.buildStack();

		verify(mockVelocityEngine).getTemplate("templates/redirectors/user_docs_redirector.yaml.vtp");
		verify(mockCloudFormationClientWrapper).waitForStackToComplete("tst-docs-synapse");
		verify(mockCloudFormationClientWrapper).describeStack("tst-docs-synapse");
		verify(mockCloudFormationClientWrapper).createOrUpdateStack(createOrUpdateStackRequestArgumentCaptor.capture());
		CreateOrUpdateStackRequest req = createOrUpdateStackRequestArgumentCaptor.getValue();
		assertEquals("tst-docs-synapse", req.getStackName());
		assertEquals("someYamlTemplate", req.getTemplateBody());
		assertEquals(expectedTags, req.getTags());

		assertTrue(optStack.isPresent());
		assertEquals("tst-docs-synapse", optStack.get().stackName());
		assertEquals(1, optStack.get().tags().size());
		assertEquals(tag, optStack.get().tags().get(0));
	}
}