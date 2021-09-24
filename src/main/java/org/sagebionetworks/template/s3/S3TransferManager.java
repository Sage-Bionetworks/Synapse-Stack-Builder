package org.sagebionetworks.template.s3;

import java.io.Closeable;

import com.amazonaws.services.s3.transfer.Copy;

public interface S3TransferManager extends Closeable {
	/**
	 * Schedules a new transfer to copy data from one Amazon S3 location to another Amazon S3 location
	 * @param sourceBucket
	 * @param sourceKey
	 * @param destinationBucket
	 * @param destinationKey
	 * @return
	 */
	Copy copy(String sourceBucket, String sourceKey, String destinationBucket, String destinationKey);
}
