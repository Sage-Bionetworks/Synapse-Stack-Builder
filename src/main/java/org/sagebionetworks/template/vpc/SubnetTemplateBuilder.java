package org.sagebionetworks.template.vpc;

public interface SubnetTemplateBuilder {
    public void buildAndDeployPublicSubnets() throws InterruptedException;
    public void buildAndDeployPrivateSubnets() throws InterruptedException;
	/**
	 * After each private subnet is built, this will build a VPC Endpoint that depends on all
	 * subnet rout table IDs.
	 * @throws InterruptedException
	 */
	void buildVPCEndpoint() throws InterruptedException;
}
