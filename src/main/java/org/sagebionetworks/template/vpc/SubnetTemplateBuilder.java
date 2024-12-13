package org.sagebionetworks.template.vpc;

public interface SubnetTemplateBuilder {
	
    void buildAndDeployPublicSubnets() throws InterruptedException;
    
    void buildAndDeployPrivateSubnets() throws InterruptedException;
    
}
