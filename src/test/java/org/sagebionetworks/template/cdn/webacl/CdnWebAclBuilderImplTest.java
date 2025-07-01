package org.sagebionetworks.template.cdn.webacl;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.stubbing.Answer;
import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.RepoConfiguration;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.Tag;

@ExtendWith(MockitoExtension.class)
public class CdnWebAclBuilderImplTest {

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
    private CdnWebAclBuilderImpl builder;

    @Test
    public void testBuildCdnWebAclStack() throws Exception {
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
        Tag tag = Tag.builder().key("aKey").value("aValue").build();
        expectedTags.add(tag);
        Stack expectedStack = Stack.builder().stackName("tst-cloudfront-webacl-stack").tags(expectedTags).build();
        when(mockStackTagsProvider.getStackTags(mockConfig)).thenReturn(expectedTags);
        when(mockCloudFormationClientWrapper.waitForStackToComplete(any(String.class))).thenReturn(Optional.of(expectedStack));
        when(mockCloudFormationClientWrapper.describeStack(any(String.class))).thenReturn(Optional.of(expectedStack));

        when(mockConfig.getProperty("org.sagebionetworks.stack")).thenReturn("tst");

        // call under test
        Optional<Stack> optStack = builder.buildCdnWebAcl();

        assertTrue(optStack.isPresent());
        assertEquals("tst-cloudfront-webacl-stack", optStack.get().stackName());
        assertEquals(1, optStack.get().tags().size());
        assertEquals(tag, optStack.get().tags().get(0));

        verify(mockVelocityEngine).getTemplate("templates/cdn/synapse-cdn-webacl.json.vtp");
        verify(mockCloudFormationClientWrapper).createOrUpdateStack(createOrUpdateStackRequestArgumentCaptor.capture());
        CreateOrUpdateStackRequest actualReq = createOrUpdateStackRequestArgumentCaptor.getValue();
        assertNotNull(actualReq);
        assertEquals("tst-cloudfront-webacl-stack", actualReq.getStackName());
        assertEquals("someYamlTemplate", actualReq.getTemplateBody());
        assertEquals(tag, actualReq.getTags().get(0));

        assertTrue(optStack.isPresent());
        assertEquals("tst-cloudfront-webacl-stack", optStack.get().stackName());
        assertEquals(1, optStack.get().tags().size());
        assertEquals(tag, optStack.get().tags().get(0));

    }

}
