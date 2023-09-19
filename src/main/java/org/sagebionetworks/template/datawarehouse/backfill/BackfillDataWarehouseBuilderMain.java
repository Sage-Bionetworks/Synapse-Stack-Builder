package org.sagebionetworks.template.datawarehouse.backfill;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.sagebionetworks.template.TemplateGuiceModule;

public class BackfillDataWarehouseBuilderMain {
    public static void main(String[] args) throws InterruptedException {
        Injector injector = Guice.createInjector(new TemplateGuiceModule());

        BackfillDataWarehouseBuilderImpl builder = injector.getInstance(BackfillDataWarehouseBuilderImpl.class);

        builder.buildAndDeploy();
    }
}
