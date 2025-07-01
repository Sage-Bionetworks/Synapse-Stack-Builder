package org.sagebionetworks.template.cdn.webacl;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.sagebionetworks.template.TemplateGuiceModule;

public class CdnWebAclBuilderMain {

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new TemplateGuiceModule());
        CdnWebAclBuilder builder = injector.getInstance(CdnWebAclBuilder.class);
        builder.build();
    }
}
