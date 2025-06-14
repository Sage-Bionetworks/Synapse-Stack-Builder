package org.sagebionetworks.template.cdn.webacl;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.CloudFormationClientWrapper;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.RepoConfiguration;
import com.amazonaws.services.cloudformation.model.Stack;

import java.io.StringWriter;
import java.util.Optional;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

public class CdnWebAclBuilderImpl implements CdnWebAclBuilder {

    private static final Logger LOGGER = LogManager.getLogger(CdnWebAclBuilderImpl.class);
    private static final String TEMPLATE_WAF_CDN = "templates/cdn/synapse-cdn-webacl.json.vtp";

    private final RepoConfiguration config;
    private final CloudFormationClientWrapper cloudFormationClientWrapper;
    private final StackTagsProvider tagsProvider;
    private final VelocityEngine velocityEngine;

    @Inject
    public CdnWebAclBuilderImpl(RepoConfiguration config, CloudFormationClientWrapper cloudFormationClientWrapper, StackTagsProvider tagsProvider, VelocityEngine velocityEngine) {
        this.config = config;
        this.cloudFormationClientWrapper = cloudFormationClientWrapper;
        this.tagsProvider = tagsProvider;
        this.velocityEngine = velocityEngine;
    }

    @Override
    public void build() {
        buildCdnWebAcl();
    }

    Optional<Stack> buildCdnWebAcl() {

        String cfStackName = String.format("%s-cloudfront-webacl-stack", config.getProperty(PROPERTY_KEY_STACK));
        Template cfTemplate = velocityEngine.getTemplate(TEMPLATE_WAF_CDN);
        VelocityContext context = new VelocityContext();
        StringWriter writer = new StringWriter();
        cfTemplate.merge(context, writer);
        String cfTemplateBody = writer.toString();
        LOGGER.info("Creating/updating stack {}", cfStackName);
        CreateOrUpdateStackRequest cfStackRequest = new CreateOrUpdateStackRequest()
                .withStackName(cfStackName)
                .withTemplateBody(cfTemplateBody)
                .withTags(tagsProvider.getStackTags(config));
        LOGGER.info("Stack request: {}", cfStackRequest);
        cloudFormationClientWrapper.createOrUpdateStack(cfStackRequest);
        try {
            cloudFormationClientWrapper.waitForStackToComplete(cfStackName);
            LOGGER.debug("Stack {} successfully created/updated", cfStackName);
        } catch (InterruptedException e) {
            throw new RuntimeException("Stack creation/update was interrupted", e);
        }
        return cloudFormationClientWrapper.describeStack(cfStackName);
    }
}
