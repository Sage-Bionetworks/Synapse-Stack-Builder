package org.sagebionetworks.template.cdn;

import com.amazonaws.services.cloudformation.model.Stack;
import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.RepoConfiguration;

import java.io.StringWriter;
import java.util.Optional;

import static org.sagebionetworks.template.Constants.CTXT_KEY_SUBDOMAIN_NAME;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_SSL_ARN;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK_INSTANCE_ALIAS;
import static org.sagebionetworks.template.Constants.CTXT_KEY_ACM_CERT_ARN;

public class CdnBuilderImpl implements CdnBuilder {

	private static final Logger logger = LogManager.getLogger(CdnBuilderImpl.class);

	private static final String TEMPLATE_STACK_CDN = "templates/cdn/synapse_cdn.yaml.vtp";

	private RepoConfiguration config;
	private VelocityEngine velocity;
	private CloudFormationClient cloudFormationClient;
	private StackTagsProvider tagsProvider;

	@Inject
	public CdnBuilderImpl(RepoConfiguration config, CloudFormationClient cloudFormationClient, StackTagsProvider tagsProvider, VelocityEngine velocity) {
		this.config = config;
		this.cloudFormationClient = cloudFormationClient;
		this.tagsProvider = tagsProvider;
		this.velocity = velocity;
	}

	@Override
	public void buildCdn() {
		buildCdnStack();
	}

	VelocityContext createContext() {
		VelocityContext ctxt = new VelocityContext();
		// The ACM ARN is the same as the one used for portal
		String acmCertificateArn = config.getProperty(PROPERTY_KEY_BEANSTALK_SSL_ARN+"portal");
		ctxt.put(CTXT_KEY_ACM_CERT_ARN, acmCertificateArn);
		String stackInstanceAlias = config.getProperty(PROPERTY_KEY_STACK_INSTANCE_ALIAS);
		ctxt.put(CTXT_KEY_SUBDOMAIN_NAME, stackInstanceAlias);
		return ctxt;
	}

	Optional<Stack> buildCdnStack() {

		VelocityContext context = createContext();
		Template template = velocity.getTemplate(TEMPLATE_STACK_CDN);
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		String cfTemplateYaml = writer.toString();
		logger.info(cfTemplateYaml);
		String cfStackName = String.format("cdn-%s-synapse", context.get(CTXT_KEY_SUBDOMAIN_NAME));
		CreateOrUpdateStackRequest cfStackRequest = new CreateOrUpdateStackRequest()
				.withStackName(cfStackName)
				.withTemplateBody(cfTemplateYaml)
				.withTags(tagsProvider.getStackTags());
		cloudFormationClient.createOrUpdateStack(cfStackRequest);
		try {
			cloudFormationClient.waitForStackToComplete(cfStackName);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return Optional.of(cloudFormationClient.describeStack(cfStackName));
	}
}
