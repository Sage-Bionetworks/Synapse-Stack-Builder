package org.sagebionetworks.template.repo.beanstalk.ssl;

public interface CertificateBuilder {
	
	/**
	 * Build or get a self-signed SSL certificate information.
	 * @return
	 */
	public CertificateInfo buildCertificate();

}
