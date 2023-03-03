package org.sagebionetworks.template.etl;

import com.amazonaws.services.cloudformation.model.Tag;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.Configuration;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.ETL_DESCRIPTORS;
import static org.sagebionetworks.template.Constants.GLUE_DATABASE_NAME;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.TEMPLATE_ETL_GLUE_JOB_RESOURCES;
import static org.sagebionetworks.template.etl.EtlBuilderImpl.GLUE_DB_NAME;

@ExtendWith(MockitoExtension.class)
public class EtlBuilderImplTest {
    private static String STACK_NAME = "dev";
    private static String INSTANCE = "test";
    @Captor
    ArgumentCaptor<CreateOrUpdateStackRequest> requestCaptor;
    @Captor
    ArgumentCaptor<VelocityContext> velocityContextCaptor;
    @Mock
    private CloudFormationClient cloudFormationClient;
    @Mock
    private VelocityEngine velocityEngine;
    @Mock
    private Configuration mockConfig;
    @Mock
    private Logger logger;
    @Mock
    private StackTagsProvider tagsProvider;
    @Mock
    private EtlConfig etlConfig;
    @Mock
    private Template mockTemplate;
    @Mock
    private LoggerFactory loggerFactory;
    private EtlBuilderImpl etlBuilderImpl;
    private List<Tag> tags = new ArrayList<>();
    private List<EtlDescriptor> etlDescriptors = new ArrayList<>();

    @BeforeEach
    public void before() {
        tags = new LinkedList<>();
        Tag t = new Tag().withKey("aKey").withValue("aValue");
        tags.add(t);
        EtlDescriptor etlDescriptor = new EtlDescriptor();
        etlDescriptor.setName("processAccessRecord");
        etlDescriptor.setScriptLocation("S3://${stack}.fakeBucket/");
        etlDescriptor.setDestinationPath("S3://${stack}.destination/");
        etlDescriptor.setSourcePath("S3://${stack}.source/");
        etlDescriptors.add(etlDescriptor);
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(STACK_NAME);
        when(mockConfig.getProperty(PROPERTY_KEY_INSTANCE)).thenReturn(INSTANCE);
        when(etlConfig.getEtlDescriptors()).thenReturn(etlDescriptors);
        when(tagsProvider.getStackTags()).thenReturn(tags);
        when(loggerFactory.getLogger(EtlBuilderImpl.class)).thenReturn(logger);

        when(velocityEngine.getTemplate(any(String.class))).thenReturn(mockTemplate);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                StringWriter writer = invocation.getArgument(1);
                writer.append("{\"fakeJson\": \"data\"}");
                return null;
            }
        }).when(mockTemplate).merge(any(), any());
        etlBuilderImpl = new EtlBuilderImpl(cloudFormationClient, velocityEngine, mockConfig, loggerFactory, tagsProvider, etlConfig);
    }

    @Test
    public void testEtlBuildAndDeployJob() {
        String expectedStackName = new StringJoiner("-")
                .add(STACK_NAME).add(INSTANCE).add("etl").toString();
        etlBuilderImpl.buildAndDeploy();
        verify(mockTemplate).merge(velocityContextCaptor.capture(), any());
        VelocityContext context = velocityContextCaptor.getValue();
        assertEquals(GLUE_DB_NAME, context.get(GLUE_DATABASE_NAME));
        assertEquals(STACK_NAME, context.get(STACK));
        assertEquals(etlDescriptors, context.get(ETL_DESCRIPTORS));
        verify(velocityEngine).getTemplate(TEMPLATE_ETL_GLUE_JOB_RESOURCES);
        verify(cloudFormationClient).createOrUpdateStack(requestCaptor.capture());
        CreateOrUpdateStackRequest req = requestCaptor.getValue();
        assertEquals(expectedStackName, req.getStackName());
        assertEquals(tags, req.getTags());
        assertNotNull(req.getTemplateBody());
    }
}
