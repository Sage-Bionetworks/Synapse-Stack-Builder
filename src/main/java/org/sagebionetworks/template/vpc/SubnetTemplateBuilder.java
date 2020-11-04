package org.sagebionetworks.template.vpc;

public interface SubnetTemplateBuilder {
    public void buildAndDeployPublicSubnets();
    public void buildAndDeployPrivateSubnets();
}
