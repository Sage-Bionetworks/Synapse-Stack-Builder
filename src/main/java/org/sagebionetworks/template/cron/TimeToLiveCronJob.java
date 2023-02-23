package org.sagebionetworks.template.cron;

import org.sagebionetworks.template.TemplateGuiceModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class TimeToLiveCronJob {

	public static void main(String[] args) {
        Injector injector = Guice.createInjector(new TemplateGuiceModule());
        ExpiredStackTeardown runner = injector.getInstance(ExpiredStackTeardown.class);
        runner.findAndDeleteExpiredStacks();
	}

}
