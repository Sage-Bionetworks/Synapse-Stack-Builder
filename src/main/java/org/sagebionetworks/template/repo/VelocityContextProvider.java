package org.sagebionetworks.template.repo;

import org.apache.velocity.VelocityContext;

public interface VelocityContextProvider {

	void addToContext(VelocityContext context);
}
