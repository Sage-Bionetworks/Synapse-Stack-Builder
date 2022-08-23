package org.sagebionetworks.template.redirectors.userdocs;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.sagebionetworks.template.TemplateGuiceModule;

public class UserDocsRedirectorBuilderMain {
	public static void main(String[] args) {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		UserDocsRedirectorBuilder builder = injector.getInstance(UserDocsRedirectorBuilder.class);
		builder.buildRedirector();
	}
}
