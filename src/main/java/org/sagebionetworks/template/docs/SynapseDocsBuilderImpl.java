package org.sagebionetworks.template.docs;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_INSTANCE;
import static org.sagebionetworks.template.Constants.DOCS_STACK_INSTANCE_FILE;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_DEV_RELEASE_DOCS_BUCKET;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_REST_DOCS_BUCKET;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;

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
	
	@Override
	public boolean deployDocs() {
		String stack = config.getProperty(PROPERTY_KEY_STACK);
		if (!stack.equals(Constants.PROD_STACK_NAME)) {
			return false;
		}
		File instanceFile = new File(DOCS_STACK_INSTANCE_FILE);
		String prodInstance = config.getProperty(PROPERTY_KEY_INSTANCE);
		// to avoid deploying docs more than once for a given prod
		try {
			FileWriter writer = new FileWriter(DOCS_STACK_INSTANCE_FILE);
			if (instanceFile.createNewFile()) {
				// new file, we write the prod instance and continue
				writer.write(prodInstance);
			} else {
				Scanner scanner = new Scanner(instanceFile);
				if (scanner.hasNextInt()) {
					// if file's instance is greater than or equal to
					// prod instance, we are up to date on docs
					int instance = scanner.nextInt();
					if (instance >= Integer.parseInt(prodInstance)) {
						writer.close();
						scanner.close();
						return false;
					}
				}
				scanner.close();
			}
			// overwrite file to the prod instance
			writer.write(prodInstance);
			writer.close();
		} catch (IOException e) {
		}
		String devDocsBucket = config.getProperty(PROPERTY_KEY_DEV_RELEASE_DOCS_BUCKET);
		String docsBucket = config.getProperty(PROPERTY_KEY_REST_DOCS_BUCKET);
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
			transferManager.copy(devDocsBucket, sourceObject.getKey(), 
					docsBucket, sourceObject.getKey());
		}
		for (String destinationObjectKey : destinationObjectKeySet) {
			s3Client.deleteObject(docsBucket, destinationObjectKey);
		}
		return true;
	}
}
