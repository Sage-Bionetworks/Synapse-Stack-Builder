package org.sagebionetworks.template.repo.beanstalk.ssl;

/**
 * Abstraction for building X.509 public key certificate key pairs.
 */
public interface CertificateBuilder {

	/**
	 * Build a new X.509 public key certificate and private key pair.
	 * 
	 * @return
	 */
	public CertificatePair buildNewX509CertificatePair();
}
