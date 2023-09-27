package org.sagebionetworks.template.cdn;

import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.util.Base16Lower;
import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.template.CloudFormationClient;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.CreateOrUpdateStackRequest;
import org.sagebionetworks.template.StackTagsProvider;
import org.sagebionetworks.template.config.RepoConfiguration;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static org.sagebionetworks.template.Constants.CTXT_KEY_SUBDOMAIN_NAME;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_BEANSTALK_SSL_ARN;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK_INSTANCE_ALIAS;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DATA_CDN_PUBLIC_KEY;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DATA_CDN_CERTIFICATE_ARN;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.CTXT_KEY_ACM_CERT_ARN;
import static org.sagebionetworks.template.Constants.CTXT_KEY_PUBLIC_KEY;
import static org.sagebionetworks.template.Constants.CTXT_KEY_PUBLIC_KEY_HASH;
import static org.sagebionetworks.template.Constants.STACK;
import static org.sagebionetworks.template.Constants.PROD_STACK_NAME;
import static org.sagebionetworks.template.Constants.DEV_STACK_NAME;

public class CdnBuilderImpl implements CdnBuilder {

	private static final Logger logger = LogManager.getLogger(CdnBuilderImpl.class);

	private static final String TEMPLATE_STACK_PORTAL_CDN = "templates/cdn/synapse_cdn.yaml.vtp";

	private static final String TEMPLATE_STACK_DATA_CDN = "templates/cdn/synapse-data-cdn.json.vtp";

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
	public void buildCdn(Type type) {
		buildCdnStack(type);
	}

	VelocityContext createContext(Type type) {
		VelocityContext ctxt = new VelocityContext();

		if (Type.PORTAL.equals(type)) {
			// The ACM ARN is the same as the one used for portal
			String acmCertificateArn = config.getProperty(PROPERTY_KEY_BEANSTALK_SSL_ARN + "portal");
			ctxt.put(CTXT_KEY_ACM_CERT_ARN, acmCertificateArn);
			String stackInstanceAlias = config.getProperty(PROPERTY_KEY_STACK_INSTANCE_ALIAS);
			ctxt.put(CTXT_KEY_SUBDOMAIN_NAME, stackInstanceAlias);
		} else if (Type.DATA.equals(type)) {
			String stack = config.getProperty(PROPERTY_KEY_STACK);
			ctxt.put(STACK, stack);
			String dataCDNPublicKey = config.getProperty(PROPERTY_KEY_DATA_CDN_PUBLIC_KEY);
			ctxt.put(CTXT_KEY_PUBLIC_KEY, dataCDNPublicKey);
			String acmCertificateArn = config.getProperty(PROPERTY_KEY_DATA_CDN_CERTIFICATE_ARN);
			ctxt.put(CTXT_KEY_ACM_CERT_ARN, acmCertificateArn);
			String subDomain = Constants.isProd(stack) ? PROD_STACK_NAME : DEV_STACK_NAME;
			ctxt.put(CTXT_KEY_SUBDOMAIN_NAME, subDomain);
			try {
				byte[] publicKey = dataCDNPublicKey.getBytes("UTF-8");
				byte[] publicKeyMD5 = MessageDigest.getInstance("MD5").digest(publicKey);
				String publicKeyHash = Base16Lower.encodeAsString(publicKeyMD5).replaceAll("[^a-zA-Z0-9]", "");
				ctxt.put(CTXT_KEY_PUBLIC_KEY_HASH, publicKeyHash);
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("Failed to calculate the MD5 of the public key: " + dataCDNPublicKey, e);
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException("Failed to calculate the MD5 of the public key: " + dataCDNPublicKey, e);
			}
		} else {
			throw new IllegalArgumentException("A valid CdnBuilder Type must be used.");
		}

		return ctxt;
	}

	Optional<Stack> buildCdnStack(Type type) {
		Template template;
		String cfStackName;
		VelocityContext context = createContext(type);

		if (Type.PORTAL.equals(type)) {
			template = velocity.getTemplate(TEMPLATE_STACK_PORTAL_CDN);
			cfStackName = String.format("cdn-%s-synapse", context.get(CTXT_KEY_SUBDOMAIN_NAME));
		} else if (Type.DATA.equals(type)){
			template = velocity.getTemplate(TEMPLATE_STACK_DATA_CDN);
			cfStackName = String.format("cdn-%s-data-synapse", context.get(STACK));
		} else {
			throw new IllegalArgumentException("A valid CdnBuilder Type must be used.");
		}

		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		String cfTemplate = writer.toString();
		logger.info(cfTemplate);
		CreateOrUpdateStackRequest cfStackRequest = new CreateOrUpdateStackRequest()
				.withStackName(cfStackName)
				.withTemplateBody(cfTemplate)
				.withTags(tagsProvider.getStackTags());
		cloudFormationClient.createOrUpdateStack(cfStackRequest);
		try {
			cloudFormationClient.waitForStackToComplete(cfStackName);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return cloudFormationClient.describeStack(cfStackName);
	}
}
