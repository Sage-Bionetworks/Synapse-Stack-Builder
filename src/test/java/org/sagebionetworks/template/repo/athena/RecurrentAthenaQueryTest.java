package org.sagebionetworks.template.repo.athena;

import static org.sagebionetworks.template.Constants.GLOBAL_RESOURCES_EXPORT_PREFIX;
import static org.sagebionetworks.template.Constants.INSTANCE;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.VPC_EXPORT_PREFIX;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class RecurrentAthenaQueryTest {
	
	private VelocityEngine velocityEngine;
	private RecurrentAthenaQueryContextProvider contextProvider;
	
	@BeforeEach
	public void before() {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		
		contextProvider = injector.getInstance(RecurrentAthenaQueryContextProvider.class);
		velocityEngine = injector.getInstance(VelocityEngine.class);
	}
	
	@Test
	public void test() {
		VelocityContext context = new VelocityContext();
		
		String stack = "dev";
		
		context.put(STACK, stack);
		context.put(INSTANCE, "101");
		context.put(VPC_EXPORT_PREFIX, Constants.createVpcExportPrefix(stack));
		context.put(GLOBAL_RESOURCES_EXPORT_PREFIX, Constants.createGlobalResourcesExportPrefix(stack));
		contextProvider.addToContext(context);
		
		Template template = this.velocityEngine.getTemplate("templates/repo/athena-queries-template.json.vpt");
		StringWriter stringWriter = new StringWriter();
		template.merge(context, stringWriter);
		// Parse the resulting template
		StringBuilder builder = new StringBuilder("{\"empty\":{}");
		builder.append(stringWriter.toString());
		builder.append("}");
		JSONObject templateJson = new JSONObject(builder.toString());
		System.out.println(templateJson.toString(5));
	}
	
}
