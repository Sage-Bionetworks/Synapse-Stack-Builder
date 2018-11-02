package org.sagebionetworks.template.repo.beanstalk.ssl;

public interface CertificateProvider {
	
	/**
	 * Provide the URLs the X.509 public key certificate and private key pair.
	 * @return
	 */
	public CertificateUrls provideCertificateUrls();

}
