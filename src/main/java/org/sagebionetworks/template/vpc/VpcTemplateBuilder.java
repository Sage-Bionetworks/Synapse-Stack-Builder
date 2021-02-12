package org.sagebionetworks.template.vpc;

public interface VpcTemplateBuilder {

	/**
	 * Build and deploy the VCP template.
	 */
	public void buildAndDeploy() throws InterruptedException;
}
