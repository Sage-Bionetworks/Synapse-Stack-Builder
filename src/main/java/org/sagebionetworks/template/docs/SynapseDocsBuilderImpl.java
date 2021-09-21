package org.sagebionetworks.template.docs;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.DOCS_STACK_INSTANCE_JSON_FILE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DEV_RELEASE_DOCS_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REST_DOCS_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_DOC_DEPLOYMENT_FLAG;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.sagebionetworks.template.Constants;
import org.sagebionetworks.template.config.RepoConfiguration;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.google.inject.Inject;

public class SynapseDocsBuilderImpl implements SynapseDocsBuilder {

	TransferManager transferManager;
	AmazonS3 s3Client;
	RepoConfiguration config;
	
	@Inject
	SynapseDocsBuilderImpl(AmazonS3 s3Client, RepoConfiguration config) {
		this.s3Client = s3Client;
		this.config = config;
		transferManager = TransferManagerBuilder
				.standard()
				.withS3Client(s3Client)
				.build();
	}
	
	boolean verifyDeployment(String docsBucket) {
		// don't deploy if flag is false
		if (!config.getBooleanProperty(PROPERTY_DOC_DEPLOYMENT_FLAG)) {
			return false;
		}
		// don't deploy on non-prod
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		if (!stack.equals(Constants.PROD_STACK_NAME)) {
			return false;
		}
		// don't deploy if the json file exists and states that it is >= to prod instance
		if (s3Client.doesObjectExist(docsBucket, DOCS_STACK_INSTANCE_JSON_FILE)) {
			String json = s3Client.getObjectAsString(docsBucket, DOCS_STACK_INSTANCE_JSON_FILE);
			JSONObject obj = new JSONObject(json);
			int instance = obj.getInt(PROPERTY_KEY_INSTANCE);
			if (instance >= Integer.parseInt(config.getProperty(PROPERTY_KEY_INSTANCE))) {
				return false;
			}
		} else { // we write the instance to the a json, and deploy
			JSONObject obj = new JSONObject();
			obj.put(PROPERTY_KEY_INSTANCE, Integer.parseInt(config.getProperty(PROPERTY_KEY_INSTANCE)));
			String json = obj.toString();
			s3Client.putObject(docsBucket, DOCS_STACK_INSTANCE_JSON_FILE, json);
		}
		return true;
	}
	
	boolean sync(String devDocsBucket, String docsBucket) {
		// deployment is a sync
		String prefix = "";
		ObjectListing sourceObjects = s3Client.listObjects(devDocsBucket, prefix);
		Stream<S3ObjectSummary> destinationObjectsStream = s3Client.listObjects(docsBucket, prefix)
				.getObjectSummaries()
				.stream();
		Set<String> destinationObjectKeySet = new HashSet<>();
		destinationObjectsStream.forEach(obj -> destinationObjectKeySet.add(obj.getKey()));
		for (S3ObjectSummary sourceObject : sourceObjects.getObjectSummaries()) {
			// make the destination set contain all objects to be removed (not updated) in the sync
			if (destinationObjectKeySet.contains(sourceObject.getKey())) {
				destinationObjectKeySet.remove(sourceObject.getKey());
			}
			// direct copy, also overwrites if keys are the same
			transferManager.copy(devDocsBucket, sourceObject.getKey(), 
					docsBucket, sourceObject.getKey());
		}
		// remove objects in the sync
		for (String destinationObjectKey : destinationObjectKeySet) {
			s3Client.deleteObject(docsBucket, destinationObjectKey);
		}
		return true;
	}
	
	@Override
	public boolean deployDocs() {
		String devDocsBucket = config.getProperty(PROPERTY_KEY_DEV_RELEASE_DOCS_BUCKET);
		String docsBucket = config.getProperty(PROPERTY_KEY_REST_DOCS_BUCKET);
		if (!verifyDeployment(docsBucket)) {
			return false;
		}
		return sync(devDocsBucket, docsBucket);
	}
}
