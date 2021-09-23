package org.sagebionetworks.template.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

public class S3TransferManagerFactoryImpl implements S3TransferManagerFactory {
	
	private AmazonS3 s3Client;
	
	public S3TransferManagerFactoryImpl(AmazonS3 s3Client) {
		super();
		this.s3Client = s3Client;
	}

	@Override
	public S3TransferManager createNewS3TransferManager() {
		return new S3TransferManagerImpl(
				TransferManagerBuilder.standard().withS3Client(s3Client).build());
	}

}
