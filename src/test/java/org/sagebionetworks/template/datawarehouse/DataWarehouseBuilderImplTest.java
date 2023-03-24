package org.sagebionetworks.template.datawarehouse;

import com.amazonaws.services.cloudformation.model.Tag;
import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.repo.kinesis.firehose.GlueTableDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DATAWAREHOUSE_GLUE_DATABASE_NAME;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

@ExtendWith(MockitoExtension.class)
public class DataWarehouseBuilderImplTest {
    private static String STACK_NAME = "dev";
    private static String DATABASE_NAME = "synapsewarehouse";
    private static String version = "v1.0.0";
    @Captor
    ArgumentCaptor<CreateOrUpdateStackRequest> requestCaptor;
    @Mock
    private CloudFormationClient cloudFormationClient;
    private VelocityEngine velocityEngine = new TemplateGuiceModule().velocityEngineProvider();
    @Mock
    private Configuration mockConfig;
    @Mock
    private Logger logger;
    @Mock
    private StackTagsProvider tagsProvider;
    @Mock
    private EtlJobConfig etlJobConfig;
    @Mock
    private LoggerFactory loggerFactory;
    private DataWarehouseBuilderImpl etlBuilderImpl;
    private List<Tag> tags = new ArrayList<>();
    private List<EtlJobDescriptor> etlJobDescriptors = new ArrayList<>();
    private EtlJobDescriptor etlJobDescriptor = new EtlJobDescriptor();

    @Test
    public void testEtlBuildAndDeployJob() {Tag t = new Tag().withKey("aKey").withValue("aValue");
        tags.add(t);
        etlJobDescriptor.setName("testjob");
        etlJobDescriptor.setScriptLocation("fakeBucket/");
        etlJobDescriptor.setScriptName("someFile.py");
        etlJobDescriptor.setSourcePath("source");
        etlJobDescriptor.setDescription("test");
        GlueTableDescriptor table = new GlueTableDescriptor();
        table.setName("someTableRef");
        table.setColumns(ImmutableMap.of("someColumn", "string"));
        etlJobDescriptor.setTableDescriptor(table);
        etlJobDescriptors.add(etlJobDescriptor);
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(STACK_NAME);
        when(mockConfig.getProperty(PROPERTY_KEY_DATAWAREHOUSE_GLUE_DATABASE_NAME)).thenReturn(DATABASE_NAME);
        when(etlJobConfig.getEtlDescriptors()).thenReturn(etlJobDescriptors);
        when(tagsProvider.getStackTags()).thenReturn(tags);
        when(loggerFactory.getLogger(DataWarehouseBuilderImpl.class)).thenReturn(logger);
        etlBuilderImpl = new DataWarehouseBuilderImpl(cloudFormationClient, velocityEngine, mockConfig, loggerFactory, tagsProvider, etlJobConfig);
        String expectedStackName = new StringJoiner("-")
                .add(STACK_NAME).add(DATABASE_NAME).add("etl-jobs").toString();

        //call under test
        etlBuilderImpl.buildAndDeploy(version);
        verify(cloudFormationClient).createOrUpdateStack(requestCaptor.capture());
        CreateOrUpdateStackRequest req = requestCaptor.getValue();
        JSONObject json = new JSONObject(req.getTemplateBody());
        assertEquals(expectedStackName, req.getStackName());
        assertEquals(tags, req.getTags());
        assertNotNull(req.getTemplateBody());
        JSONObject resources = json.getJSONObject("Resources");
        assertNotNull(resources);
        assertEquals(Set.of("AWSGlueJobRole", "synapsewarehouseGlueDatabase", "testjobGlueJob",
                        "someTableRefGlueTable", "testjobGlueJobTrigger"),
                resources.keySet());

        JSONObject props = resources.getJSONObject("testjobGlueJob").getJSONObject("Properties");
        assertEquals(DATABASE_NAME + "_"+ etlJobDescriptor.getName(), props.get("Name"));
        assertEquals(etlJobDescriptor.getDescription(), props.get("Description"));
        assertEquals("{\"--enable-continuous-cloudwatch-log\":\"true\",\"--job-bookmark-option\":" +
                        "\"job-bookmark-enable\",\"--enable-metrics\":\"true\",\"--enable-spark-ui\":\"true\"," +
                        "\"--job-language\":\"python\",\"--DATABASE_NAME\":\"synapsewarehouse\",\"--TABLE_NAME\"" +
                        ":\"someTableRef\",\"--S3_SOURCE_PATH\":\"s3://dev." + etlJobDescriptor.getSourcePath() + "\"}",
                props.getString("DefaultArguments"));

        JSONObject tableProperty = resources.getJSONObject("someTableRefGlueTable").getJSONObject("Properties");
        assertEquals("{\"Name\":\"" + etlJobDescriptor.getTableDescriptor().getName() +
                "\",\"StorageDescriptor\":{\"Columns\":[{\"Name\":\"someColumn\"," +
                "\"Type\":\"string\"}],\"InputFormat\":\"org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat\"," +
                "\"SerdeInfo\":{\"SerializationLibrary\":" + "\"org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe\"}," +
                "\"Compressed\":true,\"Location\":\"s3://dev.datawarehouse.sagebase.org/synapsewarehouse/someTableRef/\"},\"PartitionKeys\":[],\"TableType\":" +
                "\"EXTERNAL_TABLE\"}", tableProperty.getString("TableInput"));

        JSONObject dataBaseProperty = resources.getJSONObject("synapsewarehouseGlueDatabase").getJSONObject("Properties");
        assertEquals("{\"Name\":\"synapsewarehouse\"}", dataBaseProperty.getString("DatabaseInput"));

        JSONObject glueJobTrigger = resources.getJSONObject("testjobGlueJobTrigger").getJSONObject("Properties");
        assertEquals("{\"Type\":\"SCHEDULED\",\"StartOnCreation\":\"true\",\"Description\":" +
                "\"Trigger for job synapsewarehouse_testjob\",\"Name\":\"synapsewarehouse_testjob_trigger\",\"Schedule\":" +
                "\"cron(0 * * * ? *)\",\"Actions\":[{\"JobName\":\"synapsewarehouse_testjob\"}]}", glueJobTrigger.toString());
    }

    @Test
    public void testInvalidProdDatabaseName() {
        when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn("prod");
        when(mockConfig.getProperty(PROPERTY_KEY_DATAWAREHOUSE_GLUE_DATABASE_NAME)).thenReturn(null);
        etlBuilderImpl = new DataWarehouseBuilderImpl(cloudFormationClient, velocityEngine, mockConfig, loggerFactory, tagsProvider, etlJobConfig);
        //call under test
        Exception exception = assertThrows(IllegalArgumentException.class, ()-> etlBuilderImpl.buildAndDeploy(version));
        assertEquals("Database name is required.", exception.getMessage());
    }
}
