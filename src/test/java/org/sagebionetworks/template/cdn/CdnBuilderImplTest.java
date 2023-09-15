package org.sagebionetworks.template.cdn;

import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.Tag;
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
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.RepoConfiguration;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CdnBuilderImplTest {

	@Mock
	private RepoConfiguration mockConfig;

	@Mock
	private CloudFormationClient mockCloudFormationClient;

	@Mock
	private StackTagsProvider mockStackTagsProvider;

	@Mock
	private VelocityEngine mockVelocityEngine;

	@Mock
	private Template mockTemplate;

	@Captor
	private ArgumentCaptor<CreateOrUpdateStackRequest> createOrUpdateStackRequestArgumentCaptor;

	@InjectMocks
	CdnBuilderImpl builder;

	@BeforeEach
	void setUp() {
		when(mockConfig.getProperty("org.sagebionetworks.beanstalk.ssl.arn.portal")).thenReturn("acmarn");
		when(mockConfig.getProperty("org.sagebionetworks.stack.instance.alias")).thenReturn("dev");
		when(mockConfig.getProperty("org.sagebionetworks.stack")).thenReturn("tst");
		when(mockConfig.getProperty("org.sagebionetworks.cloudfront.public.key.encoded")).thenReturn("12345");
	}

	@AfterEach
	void tearDown() {
	}

	@Test
	void testCreateContext() {
		// call under test
		VelocityContext ctxt = builder.createContext();

		assertEquals("acmarn", ctxt.get("AcmCertificateArn"));
		assertEquals("dev", ctxt.get("SubDomainName"));
		assertEquals("tst", ctxt.get("stack"));
		assertEquals("12345", ctxt.get("DataCdnPublicKey"));
	}

	@Test
	void testBuildCdnStack() throws Exception{
		when(mockVelocityEngine.getTemplate(any(String.class))).thenReturn(mockTemplate);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				StringWriter writer = (StringWriter) invocation.getArgument(1);
				writer.append("someYamlTemplate");
				return null;
			}
		}).when(mockTemplate).merge(any(), any());
		List<Tag> expectedTags = new ArrayList<>();
		Tag tag = new Tag().withKey("aKey").withValue("aValue");
		expectedTags.add(tag);
		Stack expectedStack = new Stack().withStackName("cdn-dev-synapse").withTags(expectedTags);
		when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);

		when(mockCloudFormationClient.waitForStackToComplete(any(String.class))).thenReturn(Optional.of(expectedStack));
		when(mockCloudFormationClient.describeStack(any(String.class))).thenReturn(Optional.of(expectedStack));

		// call under test
		Optional<Stack> optStack = builder.buildCdnStack(CdnBuilder.Type.PORTAL);

		verify(mockVelocityEngine).getTemplate("templates/cdn/synapse_cdn.yaml.vtp");
		verify(mockCloudFormationClient).createOrUpdateStack(createOrUpdateStackRequestArgumentCaptor.capture());
		CreateOrUpdateStackRequest req = createOrUpdateStackRequestArgumentCaptor.getValue();
		assertEquals("cdn-dev-synapse", req.getStackName());
		assertEquals("someYamlTemplate", req.getTemplateBody());
		assertEquals(expectedTags, req.getTags());

		assertTrue(optStack.isPresent());
		assertEquals("cdn-dev-synapse", optStack.get().getStackName());
		assertEquals(1, optStack.get().getTags().size());
		assertEquals(tag, optStack.get().getTags().get(0));

	}

	@Test
	void testBuildDataCdnStack() throws Exception{
		when(mockVelocityEngine.getTemplate(any(String.class))).thenReturn(mockTemplate);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				StringWriter writer = (StringWriter) invocation.getArgument(1);
				writer.append("someJsonTemplate");
				return null;
			}
		}).when(mockTemplate).merge(any(), any());
		List<Tag> expectedTags = new ArrayList<>();
		Tag tag = new Tag().withKey("aKey").withValue("aValue");
		expectedTags.add(tag);
		Stack expectedStack = new Stack().withStackName("cdn-tst-data-synapse").withTags(expectedTags);
		when(mockStackTagsProvider.getStackTags()).thenReturn(expectedTags);

		when(mockCloudFormationClient.waitForStackToComplete(any(String.class))).thenReturn(Optional.of(expectedStack));
		when(mockCloudFormationClient.describeStack(any(String.class))).thenReturn(Optional.of(expectedStack));

		// call under test
		Optional<Stack> optStack = builder.buildCdnStack(CdnBuilder.Type.DATA);

		verify(mockVelocityEngine).getTemplate("templates/cdn/synapse-data-cdn.json.vtp");
		verify(mockCloudFormationClient).createOrUpdateStack(createOrUpdateStackRequestArgumentCaptor.capture());
		CreateOrUpdateStackRequest req = createOrUpdateStackRequestArgumentCaptor.getValue();
		assertEquals("cdn-tst-data-synapse", req.getStackName());
		assertEquals("someJsonTemplate", req.getTemplateBody());
		assertEquals(expectedTags, req.getTags());

		assertTrue(optStack.isPresent());
		assertEquals("cdn-tst-data-synapse", optStack.get().getStackName());
		assertEquals(1, optStack.get().getTags().size());
		assertEquals(tag, optStack.get().getTags().get(0));

	}
}