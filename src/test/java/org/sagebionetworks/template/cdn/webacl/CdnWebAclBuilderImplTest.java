package org.sagebionetworks.template.cdn.webacl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.Tag;
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
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.RepoConfiguration;

@ExtendWith(MockitoExtension.class)
public class CdnWebAclBuilderImplTest {

    @Mock
    private RepoConfiguration mockConfig;

    @Mock
    private CloudFormationClient mockCloudFormationClient;

    @Mock
    private StackTagsProvider mockStackTagsProvider;

    @Mock
    private VelocityEngine mockVelocityEngine;

    @Mock
    private TemplateLoader mockTemplateLoader;

    @Captor
    private ArgumentCaptor<CreateOrUpdateStackRequest> createOrUpdateStackRequestArgumentCaptor;

    @InjectMocks
    private CdnWebAclBuilderImpl builder;

    @Test
    public void testBuildCdnWebAclStack() throws Exception {

        List<Tag> expectedTags = new ArrayList<>();
        Tag tag = new Tag().withKey("aKey").withValue("aValue");
        expectedTags.add(tag);
        Stack expectedStack = new Stack().withStackName("tst-cloudfront-webacl-stack").withTags(expectedTags);
        when(mockStackTagsProvider.getStackTags(mockConfig)).thenReturn(expectedTags);
        when(mockTemplateLoader.loadTemplate(any(String.class))).thenReturn("someTemplate");
        when(mockCloudFormationClient.waitForStackToComplete(any(String.class))).thenReturn(Optional.of(expectedStack));
        when(mockCloudFormationClient.describeStack(any(String.class))).thenReturn(Optional.of(expectedStack));

        when(mockConfig.getProperty("org.sagebionetworks.stack")).thenReturn("tst");

        // call under test
        Optional<Stack> optStack = builder.buildCdnWebAcl();

        assertTrue(optStack.isPresent());
        assertEquals("tst-cloudfront-webacl-stack", optStack.get().getStackName());
        assertEquals(1, optStack.get().getTags().size());
        assertEquals(tag, optStack.get().getTags().get(0));

        verify(mockCloudFormationClient).createOrUpdateStack(createOrUpdateStackRequestArgumentCaptor.capture());
        CreateOrUpdateStackRequest actualReq = createOrUpdateStackRequestArgumentCaptor.getValue();
        assertNotNull(actualReq);
        assertEquals("tst-cloudfront-webacl-stack", actualReq.getStackName());
        assertEquals("someTemplate", actualReq.getTemplateBody());
        assertEquals(tag, actualReq.getTags().get(0));
    }

}
