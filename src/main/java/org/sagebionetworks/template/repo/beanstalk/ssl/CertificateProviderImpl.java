package org.sagebionetworks.template.repo.beanstalk.ssl;

import java.util.Calendar;
import java.util.StringJoiner;

import org.sagebionetworks.template.Configuration;
import org.sagebionetworks.template.Constants;

import com.amazonaws.services.s3.AmazonS3;
import com.google.inject.Inject;

/**
 * Provides S3 URLs for a X.509 public key certificate and it signing private
 * key pair.
 * 
 *
 */
public class CertificateProviderImpl implements CertificateProvider {

	private AmazonS3 s3Client;
	private Configuration configuration;
	private CertificateBuilder builder;
	
	@Inject
	public CertificateProviderImpl(AmazonS3 s3Client, Configuration configuration, CertificateBuilder builder) {
		super();
		this.s3Client = s3Client;
		this.configuration = configuration;
		this.builder = builder;
	}

	@Override
	public CertificateUrls provideCertificateUrls() {
		String bucketName = configuration.getConfigurationBucket();
		String certificateS3Key = buildCertificateS3Key();
		String rsaPrivateKeyS3Key = buildPrivateKeyS3Key();
		// Do both files exist in S3?
		if (!s3Client.doesObjectExist(bucketName, certificateS3Key)
				|| !s3Client.doesObjectExist(bucketName, rsaPrivateKeyS3Key)) {
			// Build a new certificate and upload the pair to S3.
			buildAndUploadNewCertificatePair(bucketName, certificateS3Key, rsaPrivateKeyS3Key);
		}
		String certificateUrl = createS3Url(bucketName, certificateS3Key);
		String rsaKeyUrl = createS3Url(bucketName, rsaPrivateKeyS3Key);
		return new CertificateUrls(certificateUrl, rsaKeyUrl);
	}
	
	/**
	 * Create an S3 URL for the given bucket and key.
	 * @param bucket
	 * @param key
	 * @return
	 */
	public String createS3Url(String bucket, String key) {
		// Note: URL created with s3Client.getUrl(bucket, key) results in a 404.
		StringJoiner joiner = new StringJoiner("/");
		joiner.add("https://s3.amazonaws.com");
		joiner.add(bucket);
		joiner.add(key);
		return joiner.toString();
	}

	/**
	 * Build the S3 Key for SSL certificate.
	 * 
	 * @return
	 */
	public String buildCertificateS3Key() {
		StringJoiner joiner = new StringJoiner("/");
		joiner.add(buildSSLFolder());
		joiner.add("x509-certificate.pem");
		return joiner.toString();
	}

	/**
	 * Build the S3 Key for SSL RSA private key.
	 * 
	 * @return
	 */
	public String buildPrivateKeyS3Key() {
		StringJoiner joiner = new StringJoiner("/");
		joiner.add(buildSSLFolder());
		joiner.add("rsa-private-key.pem");
		return joiner.toString();
	}

	/**
	 * Build the folder for the SSL files.
	 * 
	 * @return
	 */
	public String buildSSLFolder() {
		StringJoiner joiner = new StringJoiner("/");
		joiner.add("ssl");
		joiner.add(configuration.getProperty(Constants.PROPERTY_KEY_STACK));
		joiner.add(configuration.getProperty(Constants.PROPERTY_KEY_INSTANCE));
		Calendar calendar = Calendar.getInstance();
		joiner.add("" + calendar.get(Calendar.YEAR));
		joiner.add("" + calendar.get(Calendar.MONTH));
		return joiner.toString();
	}

	/**
	 * Build a new X.509 public key certificate and private key pair and upload both to S3.
	 * 
	 * @param bucketName
	 * @param certificateS3Key
	 * @param rsaPrivateKeyS3Key
	 */
	void buildAndUploadNewCertificatePair(String bucketName, String certificateS3Key, String rsaPrivateKeyS3Key) {
		// Build a new certificate pair
		CertificatePair pair = builder.buildNewX509CertificatePair();
		// upload both files to S3
		s3Client.putObject(bucketName, certificateS3Key, pair.getX509CertificatePEM());
		s3Client.putObject(bucketName, rsaPrivateKeyS3Key, pair.getPrivateKeyPEM());
	}
}
