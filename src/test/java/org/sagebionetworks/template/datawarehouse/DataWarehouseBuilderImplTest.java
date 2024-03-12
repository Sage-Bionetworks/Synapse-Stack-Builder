package org.sagebionetworks.template.datawarehouse;

import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.s3.AmazonS3;
import org.apache.logging.log4j.Logger;
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
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.LoggerFactory;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.Configuration;
import org.sagebionetworks.template.repo.glue.GlueColumn;
import org.sagebionetworks.template.repo.glue.GlueTableDescriptor;
import org.sagebionetworks.template.utils.ArtifactDownload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DATAWAREHOUSE_GLUE_DATABASE_NAME;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

@ExtendWith(MockitoExtension.class)
public class DataWarehouseBuilderImplTest {

	private static String STACK_NAME = "dev";
	private static String DATABASE_NAME = "SynapseWarehouse";

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
	private DataWarehouseConfig dataWarehouseConfig;
	@Mock
	private LoggerFactory loggerFactory;
	@Mock
	private ArtifactDownload mockDownloader;
	@Mock
	private AmazonS3 mockS3Client;

	private DataWarehouseBuilderImpl builder;

	private File zipFile;

	@BeforeEach
	public void before() {
		when(loggerFactory.getLogger(any())).thenReturn(logger);
		builder = new DataWarehouseBuilderImpl(cloudFormationClient, velocityEngine, mockConfig, loggerFactory, tagsProvider,
				dataWarehouseConfig, mockDownloader, mockS3Client);
	}

	@AfterEach
	public void after() {
		if (zipFile != null) {
			zipFile.delete();
		}
	}

	@Test
	public void testBuildAndDeploy() throws IOException {
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(STACK_NAME);
		when(mockConfig.getProperty(PROPERTY_KEY_DATAWAREHOUSE_GLUE_DATABASE_NAME)).thenReturn(DATABASE_NAME);

		zipFile = File.createTempFile("test", "zip");

		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));) {
			out.putNextEntry(new ZipEntry("repo-1.0.0/somethingElse.py"));
			out.putNextEntry(new ZipEntry("repo-1.0.0/src/scripts/glue_jobs/testjob.py"));
			out.putNextEntry(new ZipEntry("repo-1.0.0/src/scripts/glue_jobs/utilities/utils.py"));
		}

		when(mockDownloader.downloadFile(any())).thenReturn(zipFile);
		when(dataWarehouseConfig.getGithubRepo()).thenReturn("repo");
		when(dataWarehouseConfig.getVersion()).thenReturn("1.0.0");
		when(dataWarehouseConfig.getExtraScripts()).thenReturn(List.of("utilities/utils.py"));

		GlueColumn column = new GlueColumn();
		column.setName("someColumn");
		column.setType("string");
		column.setComment("This is test column");
		
		GlueTableDescriptor jobTable = new GlueTableDescriptor();
		jobTable.setName("testTable");
		jobTable.setDescription("Test table");
		jobTable.setColumns(Arrays.asList(column));
		
		GlueTableDescriptor anotherTable = new GlueTableDescriptor();
		anotherTable.setName("anotherTable");
		anotherTable.setDescription("Another Test table");
		anotherTable.setColumns(Arrays.asList(column));
		anotherTable.setLocation("s3://${stack}.inventory.sagebase.org/inventory/${stack}data.sagebase.org/defaultInventory/hive/");
		anotherTable.setInputFormat("org.apache.hadoop.hive.ql.io.SymlinkTextInputFormat");
		
		when(dataWarehouseConfig.getTableDescriptors()).thenReturn(List.of(jobTable, anotherTable));

		List<EtlJobDescriptor> jobs = List.of(
				new EtlJobDescriptor()
						.withName("testjob")
						.withScriptName("someFile.py")
						.withSourcePath("source")
						.withDescription("test")
						.withTargetTable("testTable")
		);

		when(dataWarehouseConfig.getEtlJobDescriptors()).thenReturn(jobs);

		List<Tag> tags = List.of(new Tag().withKey("aKey").withValue("aValue"));

		when(tagsProvider.getStackTags()).thenReturn(tags);

		String expectedStackName = new StringJoiner("-").add(STACK_NAME).add(DATABASE_NAME.toLowerCase()).add("etl-jobs").toString();

		// call under test
		builder.buildAndDeploy();

		verify(mockDownloader).downloadFile("https://codeload.github.com/Sage-Bionetworks/repo/zip/refs/tags/v1.0.0");
		verify(mockS3Client).putObject(eq("dev.aws-glue.sagebase.org"), eq("scripts/v1.0.0/testjob.py"), any(), any());
		verify(mockS3Client).putObject(eq("dev.aws-glue.sagebase.org"), eq("scripts/v1.0.0/utilities/utils.py"), any(), any());
		verifyNoMoreInteractions(mockS3Client);

		verify(cloudFormationClient).createOrUpdateStack(requestCaptor.capture());

		CreateOrUpdateStackRequest req = requestCaptor.getValue();
		JSONObject json = new JSONObject(req.getTemplateBody());
		assertEquals(expectedStackName, req.getStackName());
		assertEquals(tags, req.getTags());
		assertNotNull(req.getTemplateBody());
		JSONObject resources = json.getJSONObject("Resources");
		assertNotNull(resources);
		assertEquals(Set.of("AWSGlueJobRole", "synapsewarehouseGlueDatabase", "testjobGlueJob", "testTableGlueTable", "anotherTableGlueTable",
				"testjobGlueJobTrigger","synapsewarehouseFailedJobRule"), resources.keySet());

		JSONObject props = resources.getJSONObject("testjobGlueJob").getJSONObject("Properties");
		assertEquals(DATABASE_NAME.toLowerCase() + "_testjob", props.get("Name"));
		assertEquals("test", props.get("Description"));
		assertEquals("{"
				+ "\"--enable-continuous-cloudwatch-log\":\"true\","
				+ "\"--job-bookmark-option\":\"job-bookmark-enable\","
				+ "\"--enable-metrics\":\"true\","
				+ "\"--job-language\":\"python\","
				+ "\"--DATABASE_NAME\":\"synapsewarehouse\","
				+ "\"--TABLE_NAME\":\"testTable\","
				+ "\"--S3_SOURCE_PATH\":\"s3://dev.source\","
				+ "\"--extra-py-files\":\"s3://dev.aws-glue.sagebase.org/scripts/v1.0.0/utilities/utils.py," +
				"s3://aws-glue-studio-transforms-510798373988-prod-us-east-1/gs_explode.py," +
				"s3://aws-glue-studio-transforms-510798373988-prod-us-east-1/gs_common.py\"}", props.getString("DefaultArguments")
		);

		JSONObject tableProperty = resources.getJSONObject("testTableGlueTable").getJSONObject("Properties");
		assertEquals("{\"Name\":\"testTable"
				+ "\",\"Description\":\"Test table\",\"StorageDescriptor\":{\"Columns\":[{\"Name\":\"someColumn\","
				+ "\"Type\":\"string\",\"Comment\":\"This is test column\"}],\"InputFormat\":\"org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat\","
				+ "\"SerdeInfo\":{\"SerializationLibrary\":" + "\"org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe\"},"
				+ "\"Compressed\":true,\"Location\":\"s3://dev.datawarehouse.sagebase.org/synapsewarehouse/testTable/\"},\"PartitionKeys\":[],\"TableType\":"
				+ "\"EXTERNAL_TABLE\"}", tableProperty.getString("TableInput"));
		
		JSONObject anotherTableProperty = resources.getJSONObject("anotherTableGlueTable").getJSONObject("Properties");
		assertEquals("{\"Name\":\"anotherTable"
				+ "\",\"Description\":\"Another Test table\",\"StorageDescriptor\":{\"Columns\":[{\"Name\":\"someColumn\","
				+ "\"Type\":\"string\",\"Comment\":\"This is test column\"}],\"InputFormat\":\"org.apache.hadoop.hive.ql.io.SymlinkTextInputFormat\","
				+ "\"SerdeInfo\":{\"SerializationLibrary\":" + "\"org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe\"},"
				+ "\"Compressed\":true,\"Location\":\"s3://dev.inventory.sagebase.org/inventory/devdata.sagebase.org/defaultInventory/hive/\"},\"PartitionKeys\":[],\"TableType\":"
				+ "\"EXTERNAL_TABLE\"}", anotherTableProperty.getString("TableInput"));

		JSONObject dataBaseProperty = resources.getJSONObject("synapsewarehouseGlueDatabase").getJSONObject("Properties");
		assertEquals("{\"Name\":\"synapsewarehouse\"}", dataBaseProperty.getString("DatabaseInput"));

		JSONObject glueJobTrigger = resources.getJSONObject("testjobGlueJobTrigger").getJSONObject("Properties");
		assertEquals("{\"Type\":\"SCHEDULED\",\"StartOnCreation\":\"true\",\"Description\":"
				+ "\"Trigger for job synapsewarehouse_testjob\",\"Name\":\"synapsewarehouse_testjob_trigger\",\"Schedule\":"
				+ "\"cron(0 */6 ? * * *)\",\"Actions\":[{\"JobName\":\"synapsewarehouse_testjob\"}]}", glueJobTrigger.toString());
		
		// Rule to send email on job failure
		JSONObject eventRule = resources.getJSONObject("synapsewarehouseFailedJobRule");
		assertEquals("AWS::Events::Rule",eventRule.get("Type"));
		JSONObject eventRuleProperties = eventRule.getJSONObject("Properties");
		assertEquals("[\"aws.glue\"]", eventRuleProperties.getJSONObject("EventPattern").getJSONArray("source").toString());
		assertEquals("[\"Glue Job State Change\"]", eventRuleProperties.getJSONObject("EventPattern").getJSONArray("detail-type").toString());
		assertEquals("{\"state\":[\"FAILED\"],\"jobName\":[\"synapsewarehouse_testjob\"]}",
				eventRuleProperties.getJSONObject("EventPattern").getJSONObject("detail").toString());
		assertEquals("[{\"Id\":\"warehouse-etl-job-failure-target\","
				+ "\"Arn\":{\"Fn::ImportValue\":\"us-east-1-synapse-dev-global-resources-NotificationTopic\"}}]",
				eventRuleProperties.getJSONArray("Targets").toString());
	}

	@Test
	public void testNullDatabaseNameThrowsIllegalArgumentException() {
		when(mockConfig.getProperty(PROPERTY_KEY_DATAWAREHOUSE_GLUE_DATABASE_NAME)).thenReturn(null);

		// call under test
		Exception exception = assertThrows(IllegalArgumentException.class, () -> builder.buildAndDeploy());
		assertEquals("The database name is required and must not be the empty string.", exception.getMessage());
	}
}