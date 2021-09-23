package org.sagebionetworks.template.docs;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.DOCS_STACK_INSTANCE_JSON_FILE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_SOURCE_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_DESTINATION_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.s3.S3TransferManager;
import org.sagebionetworks.template.s3.S3TransferManagerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Copy;
import com.google.inject.Inject;

public class SynapseDocsBuilderImpl implements SynapseDocsBuilder {

	private static final Logger LOG = LogManager.getLogger(SynapseDocsBuilderImpl.class);
	
	private final S3TransferManagerFactory transferManagerFactory;
	private final AmazonS3 s3Client;
	private final RepoConfiguration config;
	
	@Inject
	SynapseDocsBuilderImpl(AmazonS3 s3Client, RepoConfiguration config, 
			S3TransferManagerFactory transferManagerFactory) {
		this.s3Client = s3Client;
		this.config = config;
		this.transferManagerFactory = transferManagerFactory;
	}
	
	boolean verifyDeployment(String destinationBucket) {
		try {
			if (!config.getBooleanProperty(PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG)) {
				LOG.info("Docs deployment flag is false, will not deploy docs.");
				return false;
			}
		} catch (Exception e) {
			LOG.info("Docs deployment flag is missing, will not deploy docs.");
			return false;
		}
		if (s3Client.doesObjectExist(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE)) {
			String json = s3Client.getObjectAsString(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE);
			JSONObject obj = new JSONObject(json);
			int instance = obj.getInt(PROPERTY_KEY_INSTANCE);
			if (instance >= Integer.parseInt(config.getProperty(PROPERTY_KEY_INSTANCE))) {
				LOG.info("Docs are up to date, will not deploy docs.");
				return false;
			}
		}
		return true;
	}
	
	void sync(String sourceBucket, String destinationBucket) {
		// deployment is a sync
		String prefix = "";
		Map<String, String> destinationKeyToETag = new HashMap<>();
		// build a map of destination object keys to their etags
		getAllS3Objects(createListObjectsRequest(destinationBucket, prefix))
			.forEach(obj -> destinationKeyToETag.put(obj.getKey(), obj.getETag()));
		// do the sync
		List<S3ObjectSummary> sourceObjects = getAllS3Objects(createListObjectsRequest(sourceBucket, prefix));
		try (S3TransferManager s3TransferManager = transferManagerFactory.createNewS3TransferManager()) {
			for (S3ObjectSummary sourceObject : sourceObjects) {
				// make the destination map contain all objects to be removed (not updated) in the sync
				String destinationETag = destinationKeyToETag.remove(sourceObject.getKey());
				if (destinationETag != null && sourceObject.getETag().equals(destinationETag)) {
					continue;
				}
				Copy cpy = s3TransferManager.copy(sourceBucket, sourceObject.getKey(), 
						destinationBucket, sourceObject.getKey());
				try {
					LOG.info("Waiting to copy " + sourceObject.getKey() + "...");
					cpy.waitForCompletion();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// remove objects in the sync
		for (String destinationObjectKey : destinationKeyToETag.keySet()) {
			s3Client.deleteObject(destinationBucket, destinationObjectKey);
		}
		
		// Write the instance to the bucket
		JSONObject obj = new JSONObject();
		obj.put(PROPERTY_KEY_INSTANCE, Integer.parseInt(config.getProperty(PROPERTY_KEY_INSTANCE)));
		String json = obj.toString();
		s3Client.putObject(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE, json);
		LOG.info("Done with sync");
	}
	
	List<S3ObjectSummary> getAllS3Objects(ListObjectsRequest listRequest) {
		List<S3ObjectSummary> objects = new LinkedList<>();
		ObjectListing listing;
		do {
			listing = s3Client.listObjects(listRequest);
			objects.addAll(listing.getObjectSummaries());
			listRequest.setMarker(listing.getNextMarker());
		} while (listing.isTruncated());
		return objects;
	}
	
	ListObjectsRequest createListObjectsRequest(String bucket, String prefix) {
		return new ListObjectsRequest().withBucketName(bucket).withPrefix(prefix);
	}
	
	@Override
	public void deployDocs(){
		String sourceBucket;
		String destinationBucket;
		try {
			sourceBucket = config.getProperty(PROPERTY_KEY_DOCS_SOURCE_BUCKET);
			destinationBucket = config.getProperty(PROPERTY_KEY_DOCS_DESTINATION_BUCKET);
		} catch (Exception e) {
			LOG.info(e.getMessage());
			return;
		}
		if (verifyDeployment(destinationBucket)) {
			sync(sourceBucket, destinationBucket);
		}
	}
	
}
