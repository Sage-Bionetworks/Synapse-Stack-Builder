package org.sagebionetworks.template.docs;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.DOCS_STACK_INSTANCE_JSON_FILE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_SOURCE_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_DESTINATION_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DOCS_DEPLOYMENT_FLAG;

import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.template.config.RepoConfiguration;

import com.google.inject.Inject;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

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
		String prefix = ""; // TODO: the code assumes no prefix, which is not true in practice

		// destination map
		Map<String, String> destinationRelKeyToETag = listRelKeyToEtag(destinationBucket, prefix);

		// copy source to destination, keep track of copied keys
		Set<String> sourceRelKeys = new HashSet<>();
		ListObjectsV2Iterable srcPages = s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder()
				.bucket(sourceBucket)
				.prefix(prefix)
				.build());

		for (ListObjectsV2Response page : srcPages) {
			for (S3Object obj : page.contents()) {

				String srcKey = obj.key();
				if (!srcKey.startsWith(prefix)) continue; // should not happen
				String relKey = srcKey.substring(prefix.length());
				sourceRelKeys.add(relKey);

				String dstKey = prefix + relKey;

				String srcEtag = obj.eTag();
				String dstEtag = destinationRelKeyToETag.get(relKey);

				if (!Objects.equals(srcEtag, dstEtag)) {
					CopyObjectRequest copyReq = CopyObjectRequest.builder()
							.sourceBucket(sourceBucket)
							.sourceKey(srcKey)
							.destinationBucket(destinationBucket)
							.destinationKey(dstKey)
							.build();
					s3Client.copyObject(copyReq);
					// Reflect the new state in our cache (helps if there are duplicate keys in listing)
					destinationRelKeyToETag.put(relKey, srcEtag);
				}
			}
		}
		List<String> toDeleteAbsKeys = destinationRelKeyToETag.keySet().stream()
				.filter(rel -> !sourceRelKeys.contains(rel))
				.map(rel -> prefix + rel)
				.collect(Collectors.toList());

		deleteInBatches(destinationBucket, toDeleteAbsKeys);

		// Write the instance to the bucket
		JSONObject obj = new JSONObject();
		obj.put(PROPERTY_KEY_INSTANCE, Integer.parseInt(config.getProperty(PROPERTY_KEY_INSTANCE)));
		String json = obj.toString();
		RequestBody requestBody = RequestBody.fromString(json);
		PutObjectRequest req = PutObjectRequest.builder().bucket(destinationBucket).key(DOCS_STACK_INSTANCE_JSON_FILE).build();
		s3Client.putObject(req, requestBody);
		LOG.info("Done with sync");
	}

	private void deleteInBatches(String bucket, List<String> keys) {
		final int MAX = 1000;
		for (int i = 0; i < keys.size(); i += MAX) {
			List<ObjectIdentifier> batch = keys.subList(i, Math.min(i + MAX, keys.size()))
					.stream()
					.map(k -> ObjectIdentifier.builder().key(k).build())
					.collect(Collectors.toList());

			if (batch.isEmpty()) continue;

			DeleteObjectsRequest delReq = DeleteObjectsRequest.builder()
					.bucket(bucket)
					.delete(Delete.builder().objects(batch).build())
					.build();

			s3Client.deleteObjects(delReq);
		}
	}

	private Map<String, String> listRelKeyToEtag(String bucket, String prefix) {
		Map<String, String> out = new HashMap<>();
		ListObjectsV2Iterable pages = s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder()
				.bucket(bucket)
				.prefix(prefix)
				.build());

		for (ListObjectsV2Response page : pages) {
			for (S3Object o : page.contents()) {
				String rel = o.key().substring(prefix.length());
				out.put(rel, o.eTag());
			}
		}
		return out;
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
