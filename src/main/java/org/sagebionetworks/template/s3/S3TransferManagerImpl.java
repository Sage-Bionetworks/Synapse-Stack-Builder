package org.sagebionetworks.template.s3;

import java.io.IOException;

import com.amazonaws.services.s3.transfer.Copy;
import com.amazonaws.services.s3.transfer.TransferManager;

public class S3TransferManagerImpl implements S3TransferManager {
	
	private TransferManager transferManager;
	
	public S3TransferManagerImpl(TransferManager transferManager) {
		super();
		this.transferManager = transferManager;
	}

	@Override
	public void close() throws IOException {
		transferManager.shutdownNow();		
	}

	@Override
	public Copy copy(String sourceBucket, String sourceKey, 
			String destinationBucket, String destinationKey) {
		return transferManager.copy(sourceBucket, sourceKey, destinationBucket, destinationKey);
	}
}
