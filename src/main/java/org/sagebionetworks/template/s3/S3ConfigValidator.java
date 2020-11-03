package org.sagebionetworks.template.s3;

public class S3ConfigValidator {
	
	private S3Config config;
	
	public S3ConfigValidator(S3Config config) {
		this.config = config;
	}
	
	public S3Config validate() {
		String inventoryBucket = config.getInventoryBucket();
	
		// Makes sure the inventory bucket is defined in the list
		if (inventoryBucket != null ) {
			config.getBuckets()
				.stream()
				.filter(bucket -> bucket.getName().equals(inventoryBucket))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("An inventory bucket is defined but was not in the list of buckets."));
		} 
		// If no inventory bucket is defined make sure that not bucket in the list requires an inventory
		else {
			config.getBuckets()
				.stream()
				.filter(bucket -> bucket.isInventoryEnabled())
				.findAny()
				.ifPresent(bucket -> {
					throw new IllegalArgumentException("The bucket " + bucket.getName() + " has the inventoryEnabled but no inventoryBucket was defined.");
				});
		}
		
		return config;
	}
	
}
