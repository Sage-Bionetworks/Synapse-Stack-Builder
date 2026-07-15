package org.sagebionetworks.template.docs;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.DOCS_STACK_INSTANCE_JSON_FILE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_SOURCE_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_DESTINATION_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.template.config.RepoConfiguration;
import com.google.inject.Inject;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

public class SynapseDocsBuilderImpl implements SynapseDocsBuilder {

	private static final Logger LOG = LogManager.getLogger(SynapseDocsBuilderImpl.class);

	private final S3Client s3Client;
	private final RepoConfiguration config;

	@Inject
	SynapseDocsBuilderImpl(S3Client s3Client, RepoConfiguration config) {
		this.s3Client = s3Client;
		this.config = config;
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
		if (doesObjectExist(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE)) {
			String json = s3Client.getObjectAsBytes(GetObjectRequest.builder()
					.bucket(destinationBucket).key(DOCS_STACK_INSTANCE_JSON_FILE).build()).asUtf8String();
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
			.forEach(obj -> destinationKeyToETag.put(obj.key(), obj.eTag()));
		// do the sync
		List<S3Object> sourceObjects = getAllS3Objects(createListObjectsRequest(sourceBucket, prefix));
		for (S3Object sourceObject : sourceObjects) {
			// make the destination map contain all objects to be removed (not updated) in the sync
			String destinationETag = destinationKeyToETag.remove(sourceObject.key());
			if (destinationETag != null && sourceObject.eTag().equals(destinationETag)) {
				continue;
			}
			LOG.info("Copying " + sourceObject.key() + "...");
			s3Client.copyObject(CopyObjectRequest.builder()
					.sourceBucket(sourceBucket).sourceKey(sourceObject.key())
					.destinationBucket(destinationBucket).destinationKey(sourceObject.key())
					.build());
		}

		// remove objects in the sync
		for (String destinationObjectKey : destinationKeyToETag.keySet()) {
			s3Client.deleteObject(DeleteObjectRequest.builder()
					.bucket(destinationBucket).key(destinationObjectKey).build());
		}

		// Write the instance to the bucket
		JSONObject obj = new JSONObject();
		obj.put(PROPERTY_KEY_INSTANCE, Integer.parseInt(config.getProperty(PROPERTY_KEY_INSTANCE)));
		String json = obj.toString();
		s3Client.putObject(PutObjectRequest.builder()
				.bucket(destinationBucket).key(DOCS_STACK_INSTANCE_JSON_FILE).build(),
				RequestBody.fromString(json));
		LOG.info("Done with sync");
	}

	List<S3Object> getAllS3Objects(ListObjectsV2Request listRequest) {
		List<S3Object> objects = new LinkedList<>();
		ListObjectsV2Response listing;
		ListObjectsV2Request request = listRequest;
		do {
			listing = s3Client.listObjectsV2(request);
			objects.addAll(listing.contents());
			request = listRequest.toBuilder().continuationToken(listing.nextContinuationToken()).build();
		} while (listing.isTruncated());
		return objects;
	}

	ListObjectsV2Request createListObjectsRequest(String bucket, String prefix) {
		return ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();
	}

	/**
	 * Does the given object already exist in S3? The v2 SDK has no direct equivalent of the v1
	 * doesObjectExist(), so a headObject is issued. A HEAD response has no body, so the SDK cannot
	 * read the error code and a missing object surfaces as a generic S3Exception with a 404 status
	 * rather than the typed NoSuchKeyException. Treat 404 as "absent" and rethrow anything else
	 * (e.g. a 403 AccessDenied must not be mistaken for a missing object).
	 */
	boolean doesObjectExist(String bucket, String key) {
		try {
			s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
			return true;
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				return false;
			}
			throw e;
		}
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
