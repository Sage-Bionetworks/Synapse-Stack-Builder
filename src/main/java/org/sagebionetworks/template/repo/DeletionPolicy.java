package org.sagebionetworks.template.repo;

/**
 * See: https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-attribute-deletionpolicy.html
 *
 */
public enum DeletionPolicy {
	Delete,
	Retain,
	Snapshot
}
