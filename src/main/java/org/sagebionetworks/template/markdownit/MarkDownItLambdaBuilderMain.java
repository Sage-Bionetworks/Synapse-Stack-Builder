package org.sagebionetworks.template.markdownit;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.sagebionetworks.template.TemplateGuiceModule;


public class MarkDownItLambdaBuilderMain {

    public static void main(String[] args) throws InterruptedException {
        Injector injector = Guice.createInjector(new TemplateGuiceModule());
        MarkDownItLambdaBuilder builder = injector.getInstance(MarkDownItLambdaBuilder.class);
        builder.buildMarkDownItLambda();
    }
}
