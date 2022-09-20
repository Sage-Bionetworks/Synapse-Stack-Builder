package org.sagebionetworks.template.dns;

import java.io.IOException;

public interface DnsBuilder {

	void buildDns(DnsConfig dnsConfig);
	void listDns(String hostedZoneId) throws IOException;

}
