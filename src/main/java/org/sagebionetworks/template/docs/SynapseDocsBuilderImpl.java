package org.sagebionetworks.template.docs;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.DOCS_STACK_INSTANCE_JSON_FILE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_SOURCE_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_DESTINATION_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.template.config.RepoConfiguration;
import org.sagebionetworks.template.s3.S3TransferManagerFactory;

import com.google.inject.Inject;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedCopy;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.CopyRequest;

public class SynapseDocsBuilderImpl implements SynapseDocsBuilder {

	private static final Logger LOG = LogManager.getLogger(SynapseDocsBuilderImpl.class);
	
	private final S3TransferManagerFactory transferManagerFactory;
	private final S3Client s3Client;
	private final RepoConfiguration config;
	
	@Inject
	SynapseDocsBuilderImpl(S3Client s3Client, RepoConfiguration config,
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
		if (doesObjectExist(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE)) {
			String json = getObjectAsString(destinationBucket, DOCS_STACK_INSTANCE_JSON_FILE);
			JSONObject obj = new JSONObject(json);
			int instance = obj.getInt(PROPERTY_KEY_INSTANCE);
			if (instance >= Integer.parseInt(config.getProperty(PROPERTY_KEY_INSTANCE))) {
				LOG.info("Docs are up to date, will not deploy docs.");
				return false;
			}
		}
		return true;
	}

	boolean doesObjectExist(String bucket, String key) {
		try {
			s3Client.headObject(request -> request
					.bucket(bucket)
					.key(key));
			return true;
		} catch (NoSuchKeyException e) {
			return false;
		}
	}

	String getObjectAsString(String bucket, String key) {
		ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(request -> request
				.bucket(bucket)
				.key(key));
		return responseBytes.asString(Charset.defaultCharset());
	}
	
	void sync(String sourceBucket, String destinationBucket) {
		// deployment is a sync
		String prefix = "";
		Map<String, String> destinationKeyToETag = new HashMap<>();
		// build a map of destination object keys to their etags
		getAllS3Objects(ListObjectsV2Request.builder().bucket(destinationBucket).prefix(prefix).build())
			.forEach(obj -> destinationKeyToETag.put(obj.key(), obj.eTag()));
		// do the sync
		List<S3Object> sourceObjects = getAllS3Objects(ListObjectsV2Request.builder().bucket(sourceBucket).build());
		try (S3TransferManager s3TransferManager = transferManagerFactory.createNewS3TransferManager()) {
			for (S3Object sourceObject : sourceObjects) {
				// make the destination map contain all objects to be removed (not updated) in the sync
				String destinationETag = destinationKeyToETag.remove(sourceObject.key());
				if (sourceObject.eTag().equals(destinationETag)) {
					continue;
				}
				CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
						.sourceBucket(sourceBucket).sourceKey(sourceObject.key())
						.destinationBucket(destinationBucket).destinationKey(sourceObject.key())
						.build();
				CopyRequest copyRequest = CopyRequest.builder().copyObjectRequest(copyObjectRequest).build();
				Copy cpy = s3TransferManager.copy(copyRequest);
				CompletedCopy completedCopy = cpy.completionFuture().join();
			}
		} catch (CompletionException e) {
			throw new RuntimeException("S3 copy operation failed", e.getCause());
		} catch (Exception e) {
			throw new RuntimeException("Failed to execute transfer", e.getCause());
		}


		// remove objects in the sync
		for (String destinationObjectKey : destinationKeyToETag.keySet()) {
			DeleteObjectRequest req = DeleteObjectRequest.builder().bucket(destinationBucket).key(destinationObjectKey).build();
			s3Client.deleteObject(req);
		}
		
		// Write the instance to the bucket
		JSONObject obj = new JSONObject();
		obj.put(PROPERTY_KEY_INSTANCE, Integer.parseInt(config.getProperty(PROPERTY_KEY_INSTANCE)));
		String json = obj.toString();
		RequestBody requestBody = RequestBody.fromString(json);
		PutObjectRequest req = PutObjectRequest.builder().bucket(destinationBucket).key(DOCS_STACK_INSTANCE_JSON_FILE).build();
		s3Client.putObject(req, requestBody);
		LOG.info("Done with sync");
	}
	
	List<S3Object> getAllS3Objects(ListObjectsV2Request listRequest) {
		return s3Client.listObjectsV2Paginator(listRequest)
				.contents()
				.stream()
				.collect(Collectors.toList());
	}
	
	ListObjectsV2Request createListObjectsRequest(String bucket, String prefix) {
		return ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();
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
