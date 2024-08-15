package org.sagebionetworks.template.agent;

import java.io.IOException;

import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class BedrockAgentMain {

	public static void main(String[] args) throws IOException {
        Injector injector = Guice.createInjector(new TemplateGuiceModule());
        AgentBuilder builder = injector.getInstance(AgentBuilder.class);
        builder.buildAndDeploy();
	}

}
