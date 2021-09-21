package org.sagebionetworks.template.docs;

public interface SynapseDocsBuilder {
	/**
	 * Deploys the Synapse Docs.
	 */
	boolean deployDocs();
	
	/**
	 * Syncs the doc bucket with the dev bucket
	 * @param devBucket
	 * @param docsBucket
	 * @return
	 */
	boolean sync(String devBucket, String docsBucket);
	
	/**
	 * Verifies whether we can deploy to the docs bucket
	 * @param docsBucket
	 * @return
	 */
	boolean verifyDeployment(String docsBucket);
}
