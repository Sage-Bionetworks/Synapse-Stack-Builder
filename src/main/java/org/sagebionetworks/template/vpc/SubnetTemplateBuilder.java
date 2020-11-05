package org.sagebionetworks.template.vpc;

public interface SubnetTemplateBuilder {
    public void buildAndDeployPublicSubnets() throws InterruptedException;
    public void buildAndDeployPrivateSubnets() throws InterruptedException;
}
