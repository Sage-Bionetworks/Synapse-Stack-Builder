package org.sagebionetworks.template.agent;

import java.io.IOException;

public interface AgentBuilder {

	void buildAndDeploy() throws IOException;
}
