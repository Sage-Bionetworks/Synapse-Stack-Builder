package org.sagebionetworks.template.repo;

public interface RepositoryTemplateBuilder {
	
	/**
	 * Build the repository template and deploy the stack.
	 * @throws InterruptedException 
	 */
	public void buildAndDeploy() throws InterruptedException;

}
