package org.sagebionetworks.template.agent;

import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class BedrockAgentMain {

	public static void main(String[] args) {
        Injector injector = Guice.createInjector(new TemplateGuiceModule());
        AgentBuilder builder = injector.getInstance(AgentBuilder.class);
        builder.buildAndDeploy();
	}

}
