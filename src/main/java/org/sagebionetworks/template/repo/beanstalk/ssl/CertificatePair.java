package org.sagebionetworks.template.repo.beanstalk.ssl;

/**
 * 
 * A X.509 public key certificate and the private key used to sign the
 * certificate. Both the certificate and private key stored as Privacy-Enhanced
 * Mail (PEM) strings.
 *
 */
public class CertificatePair {

	String x509CertificatePEM;
	String privateKeyPEM;

	/**
	 * 
	 * @param x509CertificatePEM X.509 public key certificate as PEM string.
	 * @param privateKeyPEM      Private key used to sign the certificate as PEM string.
	 */
	public CertificatePair(String x509CertificatePEM, String privateKeyPEM) {
		super();
		this.x509CertificatePEM = x509CertificatePEM;
		this.privateKeyPEM = privateKeyPEM;
	}

	/**
	 * X.509 public key certificate as PEM string.
	 * @return
	 */
	public String getX509CertificatePEM() {
		return x509CertificatePEM;
	}

	/**
	 * Private key used to sign the certificate as PEM string.
	 * @return
	 */
	public String getPrivateKeyPEM() {
		return privateKeyPEM;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((privateKeyPEM == null) ? 0 : privateKeyPEM.hashCode());
		result = prime * result + ((x509CertificatePEM == null) ? 0 : x509CertificatePEM.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CertificatePair other = (CertificatePair) obj;
		if (privateKeyPEM == null) {
			if (other.privateKeyPEM != null)
				return false;
		} else if (!privateKeyPEM.equals(other.privateKeyPEM))
			return false;
		if (x509CertificatePEM == null) {
			if (other.x509CertificatePEM != null)
				return false;
		} else if (!x509CertificatePEM.equals(other.x509CertificatePEM))
			return false;
		return true;
	}

}
