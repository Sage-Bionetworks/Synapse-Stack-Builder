package org.sagebionetworks.template.repo.queues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sagebionetworks.template.Constants.GLOBAL_RESOURCES_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.INSTANCE;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.VPC_EXPORT_PREFIX;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class SnsAndSqsTest {
	
	VelocityEngine velocityEngine;
	
	SnsAndSqsVelocityContextProvider contextProvider;

	@Before
	public void before() {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		contextProvider = injector.getInstance(SnsAndSqsVelocityContextProvider.class);
		velocityEngine = injector.getInstance(VelocityEngine.class);
	}
	
	@Test
	public void testTemplateProdAlarms() {
		String stack = "prod";
		// call under test
		JSONObject json = mergeTemplateForStack(stack);
		// the prod stack should have the following alarms:
		assertTrue(json.has("QueryOldestMessageAlarm"));
		assertTrue(json.has("QueryNextPageOldestMessageAlarm"));
		assertTrue(json.has("AddFilesToDownloadListOldestMessageAlarm"));
		assertTrue(json.has("DoiOldestMessageAlarm"));
		assertTrue(json.has("StorageReportOldestMessageAlarm"));
	}
	
	@Test
	public void testTemplateDevAlarms() {
		String stack = "dev";
		// call under test
		JSONObject json = mergeTemplateForStack(stack);
		// dev stack should not have alarms (PLFM-5768).
		assertFalse(json.toString().contains("AWS::CloudWatch::Alarm"));
	}
	
	@Test
	public void testFifoQueueDeadLetter() {
		JSONObject json = mergeTemplateForStack("dev");
		JSONObject queueProps = json.getJSONObject("GridSnapshotExportFifoQueue").getJSONObject("Properties");
		JSONObject redrive = queueProps.getJSONObject("RedrivePolicy");
		assertEquals("30", redrive.get("maxReceiveCount").toString());
		assertEquals("GridSnapshotExportFifoDeadLetter",
				redrive.getJSONObject("deadLetterTargetArn").getJSONArray("Fn::GetAtt").getString(0));
		JSONObject dlqProps = json.getJSONObject("GridSnapshotExportFifoDeadLetter").getJSONObject("Properties");
		assertTrue(dlqProps.getBoolean("FifoQueue"));
		assertEquals("dev-101-GRID_SNAPSHOT_EXPORT-dead-letter.fifo", dlqProps.getString("QueueName"));
	}

	@Test
	public void testStandardQueueDeadLetter() {
		JSONObject json = mergeTemplateForStack("dev");
		JSONObject dlqProps = json.getJSONObject("WebhookMessageDeadLetter").getJSONObject("Properties");
		assertFalse(dlqProps.has("FifoQueue"));
		assertEquals(1, dlqProps.length());
		assertEquals("dev-101-WEBHOOK_MESSAGE-dead-letter", dlqProps.getString("QueueName"));
	}

	/**
	 * Merge the template to the context using the provided stack.
	 * @param stack
	 * @return
	 */
	private JSONObject mergeTemplateForStack(String stack) {
		VelocityContext context = new VelocityContext();
		context.put(STACK, stack);
		context.put(INSTANCE, "101");
		context.put(VPC_EXPORT_PREFIX, Constants.createVpcExportPrefix(stack));
		context.put(GLOBAL_RESOURCES_EXPORT_PREFIX, Constants.createGlobalResourcesExportPrefix(stack));
		contextProvider.addToContext(context);
		
		Template template = this.velocityEngine.getTemplate("templates/repo/sns-and-sqs-template.json.vpt");
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		// Parse the resulting template
		StringBuilder builder = new StringBuilder("{\"empty\":{}");
		builder.append(stringWriter.toString());
		builder.append("}");
		JSONObject templateJson = new JSONObject(builder.toString());
		System.out.println(templateJson.toString(5));
		return templateJson;
	}
}
