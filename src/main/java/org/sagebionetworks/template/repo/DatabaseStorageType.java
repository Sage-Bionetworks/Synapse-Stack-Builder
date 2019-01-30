package org.sagebionetworks.template.repo;

/**
 * Possible Storage types: https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_CreateDBInstance.html
 *
 */
public enum DatabaseStorageType {

	standard,
	gp2,
	io1
}
