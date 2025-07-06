package org.sagebionetworks.template.s3;


import software.amazon.awssdk.transfer.s3.S3TransferManager;

public interface S3TransferManagerFactory {
	/**
	 * Creates an S3TransferManager
	 * @return
	 */
	S3TransferManager createNewS3TransferManager();
}
