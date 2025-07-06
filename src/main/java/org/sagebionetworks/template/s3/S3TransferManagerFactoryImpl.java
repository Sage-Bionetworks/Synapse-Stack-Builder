package org.sagebionetworks.template.s3;

import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

public class S3TransferManagerFactoryImpl implements S3TransferManagerFactory {
	
	private S3AsyncClient s3Client;
	
	public S3TransferManagerFactoryImpl(S3AsyncClient s3Client) {
		super();
		this.s3Client = s3Client;
	}

	@Override
	public S3TransferManager createNewS3TransferManager() {
		return software.amazon.awssdk.transfer.s3.S3TransferManager.builder().s3Client(s3Client).build();
	}

}
