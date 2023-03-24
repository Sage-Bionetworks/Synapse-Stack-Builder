package org.sagebionetworks.template.datawarehouse;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.sagebionetworks.template.TemplateGuiceModule;

public class DataWarehouseBuilderMain {
    public static void main(String[] args) throws InterruptedException {
        Injector injector = Guice.createInjector(new TemplateGuiceModule());
        DataWarehouseBuilder builder = injector.getInstance(DataWarehouseBuilder.class);
        GithubCopy githubCopy = injector.getInstance(GithubCopy.class);
        String version = githubCopy.copyFileFromGithub();
        builder.buildAndDeploy(version);
    }
}
