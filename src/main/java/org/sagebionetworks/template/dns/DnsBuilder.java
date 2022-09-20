package org.sagebionetworks.template.dns;

public interface DnsBuilder {

	void buildDns(DnsConfig dnsConfig);
	void listDns(String hostedZoneId);

}
