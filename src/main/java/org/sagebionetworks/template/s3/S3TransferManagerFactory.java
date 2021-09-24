package org.sagebionetworks.template.s3;

public interface S3TransferManagerFactory {
	/**
	 * Creates an S3TransferManager
	 * @return
	 */
	S3TransferManager createNewS3TransferManager();
}
