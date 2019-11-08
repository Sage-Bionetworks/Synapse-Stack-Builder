package org.sagebionetworks.template.repo;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_S3_BUCKETS_CSV;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;

import org.sagebionetworks.template.TemplateGuiceModule;
import org.sagebionetworks.template.config.RepoConfiguration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.ServerSideEncryptionByDefault;
import com.amazonaws.services.s3.model.ServerSideEncryptionConfiguration;
import com.amazonaws.services.s3.model.ServerSideEncryptionRule;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class S3BucketBuilderImpl implements S3BucketBuilder {

	AmazonS3 s3Client;
	RepoConfiguration config;

	@Inject
	public S3BucketBuilderImpl(AmazonS3 s3Client, RepoConfiguration configuration) {
		super();
		this.s3Client = s3Client;
		this.config = configuration;
	}

	@Override
	public void buildAllBuckets() {
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		String[] buckets = config.getComaSeparatedProperty(PROPERTY_KEY_S3_BUCKETS_CSV);
		for (String rawName : buckets) {
			String bucketName = rawName.replace("${stack}", stack);
			if(bucketName.contains("$")) {
				throw new IllegalArgumentException("Unable to read bucket name: "+bucketName);
			}
			System.out.println("Creating bucket: "+bucketName);
			s3Client.createBucket(bucketName);
			try {
				// If server side encryption is not currently set this call with throw a 404
				s3Client.getBucketEncryption(bucketName);
			} catch (AmazonServiceException e) {
				if(e.getStatusCode() == 404) {
					// The bucket is not currently encrypted so configure it for encryption.
					System.out.println("Setting server side encryption for bucket: "+bucketName);
					s3Client.setBucketEncryption(new SetBucketEncryptionRequest().withBucketName(bucketName)
							.withServerSideEncryptionConfiguration(new ServerSideEncryptionConfiguration()
									.withRules(new ServerSideEncryptionRule().withApplyServerSideEncryptionByDefault(
											new ServerSideEncryptionByDefault().withSSEAlgorithm(SSEAlgorithm.AES256)))));
				}else {
					throw e;
				}
			} 
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		Injector injector = Guice.createInjector(new TemplateGuiceModule());
		S3BucketBuilder builder = injector.getInstance(S3BucketBuilder.class);
		builder.buildAllBuckets();
	}

}
