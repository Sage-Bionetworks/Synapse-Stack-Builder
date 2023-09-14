package org.sagebionetworks.template.cdn;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.sagebionetworks.template.TemplateGuiceModule;

public class DataCdnBuilderMain {

	public static void main(String[] args) {

		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		CdnBuilder builder = injector.getInstance(CdnBuilder.class);
		builder.buildCdn(CdnBuilder.Type.DATA);

	}
}
