package org.sagebionetworks.template.docs;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.DOCS_STACK_INSTANCE_JSON_FILE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_SOURCE_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_DESTINATION_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.sagebionetworks.template.config.RepoConfiguration;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Copy;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.google.inject.Inject;

public class SynapseDocsBuilderImpl implements SynapseDocsBuilder {

	TransferManager transferManager;
	AmazonS3 s3Client;
	RepoConfiguration config;
	
	@Inject
	SynapseDocsBuilderImpl(AmazonS3 s3Client, RepoConfiguration config, TransferManager transferManager) {
		this.s3Client = s3Client;
		this.config = config;
		this.transferManager = transferManager;
	}
	
	boolean verifyDeployment(String destinationBucket) {
		// don't deploy if flag is false
		try {
			if (!config.getBooleanProperty(PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG)) {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
		// don't deploy if the json file exists and states that it is >= to prod instance
		if (s3Client.doesObjectExist(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE)) {
			String json = s3Client.getObjectAsString(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE);
			JSONObject obj = new JSONObject(json);
			int instance = obj.getInt(PROPERTY_KEY_INSTANCE);
			if (instance >= Integer.parseInt(config.getProperty(PROPERTY_KEY_INSTANCE))) {
				return false;
			}
		}
		return true;
	}
	
	void sync(String sourceBucket, String destinationBucket) {
		// deployment is a sync
		String prefix = "";
		ListObjectsRequest destinationListRequest = new ListObjectsRequest()
				.withBucketName(destinationBucket)
				.withPrefix(prefix);
		ObjectListing destinationListing;
		Map<String, String> destinationKeyToETag = new HashMap<>();
		// build a map of destination object keys to their etags
		do {
			destinationListing = s3Client.listObjects(destinationListRequest);
			destinationListing.getObjectSummaries()
				.forEach(obj -> destinationKeyToETag.put(obj.getKey(), obj.getETag()));
			destinationListRequest.setMarker(destinationListing.getNextMarker());
		} while (destinationListing.isTruncated());
		
		// do the sync
		ListObjectsRequest sourceListRequest = new ListObjectsRequest()
				.withBucketName(sourceBucket)
				.withPrefix(prefix);
		ObjectListing sourceListing;
		do {
			sourceListing = s3Client.listObjects(sourceListRequest);
			for (S3ObjectSummary sourceObject : sourceListing.getObjectSummaries()) {
				// make the destination map contain all objects to be removed (not updated) in the sync
				String destinationETag = destinationKeyToETag.remove(sourceObject.getKey());
				// if the source object's key is at the destination and it has the same etag, don't copy
				if (destinationETag != null && sourceObject.getETag().equals(destinationETag)) {
					continue;
				}
				Copy cpy = transferManager.copy(sourceBucket, sourceObject.getKey(), 
						destinationBucket, sourceObject.getKey());
				try {
					cpy.waitForCompletion();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			sourceListRequest.setMarker(sourceListing.getNextMarker());
		} while (sourceListing.isTruncated());
		
		// remove objects in the sync
		for (String destinationObjectKey : destinationKeyToETag.keySet()) {
			s3Client.deleteObject(destinationBucket, destinationObjectKey);
		}
		
		// Write the instance to the bucket
		JSONObject obj = new JSONObject();
		obj.put(PROPERTY_KEY_INSTANCE, Integer.parseInt(config.getProperty(PROPERTY_KEY_INSTANCE)));
		String json = obj.toString();
		s3Client.putObject(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE, json);
	}
	
	@Override
	public void deployDocs(){
		String sourceBucket = config.getProperty(PROPERTY_KEY_DOCS_SOURCE_BUCKET);
		String destinationBucket = config.getProperty(PROPERTY_KEY_DOCS_DESTINATION_BUCKET);
		if (verifyDeployment(destinationBucket)) {
			sync(sourceBucket, destinationBucket);
		}
	}
}
