package org.sagebionetworks.template.repo.appconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.template.Constants.INSTANCE;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.VPC_EXPORT_PREFIX;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.sagebionetworks.template.TemplateUtils;

public class AppConfigTest {
    VelocityEngine velocityEngine;
    AppConfigVelocityContextProvider contextProvider;
    @Test
    public void testTemplateDevAppConfig() {
        Injector injector = Guice.createInjector(new TemplateGuiceModule());
        contextProvider = injector.getInstance(AppConfigVelocityContextProvider.class);
        velocityEngine = injector.getInstance(VelocityEngine.class);
        String stack = "dev";
        String json = mergeTemplateForStack(stack).toString(4);
        String expectedJson = new JSONObject(TemplateUtils.loadContentFromFile("appconfig/appconfig-data-test.json")).toString(4);
        assertEquals(expectedJson, json);
    }
    private JSONObject mergeTemplateForStack(String stack) {
        VelocityContext context = new VelocityContext();
        context.put(STACK, stack);
        context.put(INSTANCE, "101");
        context.put(VPC_EXPORT_PREFIX, Constants.createVpcExportPrefix(stack));
        contextProvider.addToContext(context);

        Template template = this.velocityEngine.getTemplate("templates/repo/appconfig-template.json.vpt");
        StringWriter stringWriter = new StringWriter();
        template.merge(context, stringWriter);
        // Parse the resulting template
        System.out.println(stringWriter.toString());
        StringBuilder builder = new StringBuilder("{\"empty\":{}");
        builder.append(stringWriter.toString());
        builder.append("}");
        JSONObject templateJson = new JSONObject(builder.toString());
        System.out.println(templateJson.toString(5));
        return templateJson;
    }
}
