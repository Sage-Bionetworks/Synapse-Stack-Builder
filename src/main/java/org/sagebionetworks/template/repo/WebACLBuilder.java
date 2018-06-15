package org.sagebionetworks.template.repo;

import java.util.List;

public interface WebACLBuilder {

	/**
	 * Build the Web ACL and associate it with each provided environment.
	 * 
	 * @param environmentNames
	 */
	public void buildWebACL(List<String> environmentNames);
}
