package org.sagebionetworks.template.cdn.webacl;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.RepoConfiguration;
import com.amazonaws.services.cloudformation.model.Stack;

import java.util.Optional;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

public class CdnWebAclBuilderImpl implements CdnWebAclBuilder {

    private static final Logger LOGGER = LogManager.getLogger(CdnWebAclBuilderImpl.class);
    private static final String TEMPLATE_WAF_CDN = "templates/cdn/synapse-cdn-webacl.json";

    private final RepoConfiguration config;
    private final CloudFormationClient cloudFormationClient;
    private final StackTagsProvider tagsProvider;
    private final TemplateLoader templateLoader;

    @Inject
    public CdnWebAclBuilderImpl(RepoConfiguration config, CloudFormationClient cloudFormationClient, StackTagsProvider tagsProvider, TemplateLoader templateLoader) {
        this.config = config;
        this.cloudFormationClient = cloudFormationClient;
        this.tagsProvider = tagsProvider;
        this.templateLoader = templateLoader;
    }

    @Override
    public void build() {
        buildCdnWebAcl();
    }

    Optional<Stack> buildCdnWebAcl() {

        String cfStackName = String.format("%s-cloudfront-webacl-stack", config.getProperty(PROPERTY_KEY_STACK));
        String cfTemplate = templateLoader.loadTemplate(TEMPLATE_WAF_CDN);
        LOGGER.info("Creating/updating stack {}", cfStackName);
        CreateOrUpdateStackRequest cfStackRequest = new CreateOrUpdateStackRequest()
                .withStackName(cfStackName)
                .withTemplateBody(cfTemplate)
                .withTags(tagsProvider.getStackTags(config));
        LOGGER.info("Stack request: {}", cfStackRequest);
        cloudFormationClient.createOrUpdateStack(cfStackRequest);
        try {
            cloudFormationClient.waitForStackToComplete(cfStackName);
            LOGGER.debug("Stack {} successfully created/updated", cfStackName);
        } catch (InterruptedException e) {
            throw new RuntimeException("Stack creation/update was interrupted", e);
        }
        return cloudFormationClient.describeStack(cfStackName);
    }
}
