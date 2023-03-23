package org.sagebionetworks.template.etl;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.sagebionetworks.template.TemplateGuiceModule;

public class EtlBuilderMain {
    public static void main(String[] args) throws InterruptedException {
        String databaseName = args[0];
        Injector injector = Guice.createInjector(new TemplateGuiceModule());
        EtlBuilder builder = injector.getInstance(EtlBuilder.class);
        GithubCopy githubCopy = injector.getInstance(GithubCopy.class);
        String version = githubCopy.copyFileFromGithub();
        builder.buildAndDeploy(version, databaseName.toLowerCase());
    }
}
