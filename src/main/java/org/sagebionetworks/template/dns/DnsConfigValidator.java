package org.sagebionetworks.template.dns;

import java.io.IOException;

public interface DnsConfigValidator {

	void validate(DnsConfig configFile) throws IOException;

}
