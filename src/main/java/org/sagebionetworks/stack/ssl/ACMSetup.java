package org.sagebionetworks.stack.ssl;

import com.amazonaws.services.certificatemanager.AWSCertificateManagerClient;
import com.amazonaws.services.certificatemanager.model.GetCertificateRequest;
import com.amazonaws.services.certificatemanager.model.GetCertificateResult;
import org.sagebionetworks.stack.GeneratedResources;
import org.sagebionetworks.stack.ResourceProcessor;
import org.sagebionetworks.stack.StackEnvironmentType;
import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

public class ACMSetup implements ResourceProcessor {
	
	private AWSCertificateManagerClient acmClient;
	private InputConfiguration config;
	private GeneratedResources resources;

	public ACMSetup(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		super();
		this.initialize(factory, config, resources);
	}
	
	@Override
	public void initialize(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		this.acmClient = factory.createCertificateManagerClient();
		this.config = config;
		this.resources = resources;
	}

	@Override
	public void setupResources() throws InterruptedException {
		for (StackEnvironmentType t: StackEnvironmentType.values()) {
			setupResource(t);
		}
	}
	
	/**
	 * Note: No setup per se, the ARNs are already created,
	 *		 we just check that we can get the certs using the ARNs.
	 * @param env 
	 */
	private void setupResource(StackEnvironmentType env) {
		String amcCertificateArn = config.getACMCertificateArn(env);
		GetCertificateRequest req = new GetCertificateRequest().withCertificateArn(amcCertificateArn);
		// Should get ResourceNotFoundException if problem
		GetCertificateResult res = acmClient.getCertificate(req);
		resources.setACMCertificateArn(env, amcCertificateArn);
	}

	@Override
	public void teardownResources() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}
