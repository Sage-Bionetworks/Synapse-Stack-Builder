package org.sagebionetworks.template.global;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.sagebionetworks.template.TemplateGuiceModule;

public class GlobalResourcesBuilderMain {

    public static void main(String[] args) throws InterruptedException {
        Injector injector = Guice.createInjector(new TemplateGuiceModule());
        GlobalResourcesBuilder builder = injector.getInstance(GlobalResourcesBuilder.class);
        builder.buildGlobalResources();
    }

}
