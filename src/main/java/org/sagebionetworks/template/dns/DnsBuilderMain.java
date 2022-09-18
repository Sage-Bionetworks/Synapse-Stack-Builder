package org.sagebionetworks.template.dns;

import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.io.IOUtils;
import org.sagebionetworks.template.Route53Client;
import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.TemplateUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public class DnsBuilderMain {


	public static void main(String[] args) throws Exception {

		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		DnsConfig config = injector.getInstance(DnsConfig.class);
		//DnsConfig config = TemplateUtils.loadFromJsonFile("/templates/dns/dns.json", DnsConfig.class);
		//InputStream in = DnsBuilderMain.class.getResourceAsStream("/templates/dns/dns.json");
		// \String s = IOUtils.toString(in, StandardCharsets.UTF_8.name());
//		ObjectMapper OBJECT_MAPPER = new ObjectMapper();
//		DnsConfig config = OBJECT_MAPPER.readValue(DnsBuilderMain.class.getResource("/templates/dns/dns.json"), DnsConfig.class);
		System.out.println(config);
	}

}
