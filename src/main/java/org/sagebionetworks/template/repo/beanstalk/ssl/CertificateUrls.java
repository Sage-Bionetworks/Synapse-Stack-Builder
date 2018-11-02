package org.sagebionetworks.template.repo.beanstalk.ssl;

/**
 * S3 URLs for an X.509 certificate and its private key. 
 */
public class CertificateUrls {

	private String x509CertificateUrl;
	private String privateKeyUrl;

	/**
	 * 
	 * @param x509CertificateUrl The S3 URL of the self-signed X.509 certificate.
	 * @param privateKeyUrl      The S3 URL of the private key used to sign the
	 *                           X.509 certificate.
	 */
	public CertificateUrls(String x509CertificateUrl, String privateKeyUrl) {
		super();
		this.x509CertificateUrl = x509CertificateUrl;
		this.privateKeyUrl = privateKeyUrl;
	}

	/**
	 * The S3 URL of the self-signed X.509 certificate.
	 * 
	 * @return
	 */
	public String getX509CertificateUrl() {
		return x509CertificateUrl;
	}

	/**
	 * The S3 URL of the private key used to sign the X.509 certificate.
	 * 
	 * @return
	 */
	public String getPrivateKeyUrl() {
		return privateKeyUrl;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((privateKeyUrl == null) ? 0 : privateKeyUrl.hashCode());
		result = prime * result + ((x509CertificateUrl == null) ? 0 : x509CertificateUrl.hashCode());
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
		CertificateUrls other = (CertificateUrls) obj;
		if (privateKeyUrl == null) {
			if (other.privateKeyUrl != null)
				return false;
		} else if (!privateKeyUrl.equals(other.privateKeyUrl))
			return false;
		if (x509CertificateUrl == null) {
			if (other.x509CertificateUrl != null)
				return false;
		} else if (!x509CertificateUrl.equals(other.x509CertificateUrl))
			return false;
		return true;
	}

}
