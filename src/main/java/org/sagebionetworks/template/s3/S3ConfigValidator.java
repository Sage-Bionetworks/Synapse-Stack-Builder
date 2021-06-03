package org.sagebionetworks.template.s3;

import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.util.ValidateArgument;

import com.amazonaws.services.s3.model.StorageClass;

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
		
		config.getBuckets().stream().forEach(this::validateStorageClassTransitions);
		
		
		return config;
	}
	
	private void validateStorageClassTransitions(S3BucketDescriptor bucket) {
		if (bucket.getStorageClassTransitions() == null || bucket.getStorageClassTransitions().isEmpty()) {
			return;
		}
		
		Set<StorageClass> classes = new HashSet<>();
		
		for (S3BucketClassTransition transition : bucket.getStorageClassTransitions()) {
			ValidateArgument.required(transition.getStorageClass(), "The storageClass for the transition in bucket " + bucket.getName());
			ValidateArgument.requirement(transition.getDays() != null && transition.getDays() > 0, "The days value must be greater than 0 for transition in bucket " + bucket.getName());
			ValidateArgument.requirement(classes.add(transition.getStorageClass()), "Duplicate storageClass transition found for bucket " + bucket.getName() + ": " + transition);
		}
	}
	
}
