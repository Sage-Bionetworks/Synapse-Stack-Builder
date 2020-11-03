package org.sagebionetworks.template.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class S3ConfigValidatorTest {

	@Mock
	private S3Config mockConfig;

	@InjectMocks
	private S3ConfigValidator validator;

	@Test
	public void testValidate() {
		String inventoryBucketName = "inventory";

		S3BucketDescriptor inventoryBucket = new S3BucketDescriptor();
		inventoryBucket.setName(inventoryBucketName);

		S3BucketDescriptor anotherBucket = new S3BucketDescriptor();
		anotherBucket.setName("antoherBucket");
		anotherBucket.setInventoryEnabled(true);

		when(mockConfig.getInventoryBucket()).thenReturn(inventoryBucketName);
		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(inventoryBucket, anotherBucket));

		// Call under test
		validator.validate();
	}

	@Test
	public void testValidateWithNoInventoryDefinedAndInventoryEnabled() {
		String inventoryBucketName = null;

		S3BucketDescriptor anotherBucket = new S3BucketDescriptor();
		anotherBucket.setName("antoherBucket");
		
		// The inventory is enabled for this bucket, but not inventory bucket was defined
		anotherBucket.setInventoryEnabled(true);

		when(mockConfig.getInventoryBucket()).thenReturn(inventoryBucketName);
		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(anotherBucket));

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			validator.validate();
		}).getMessage();

		assertEquals("The bucket antoherBucket has the inventoryEnabled but no inventoryBucket was defined.", errorMessage);
	}
	
	@Test
	public void testValidateWithInventoryDefinedAndNoBucket() {
		// The inventory bucket is defined, but it's not defined in the list of buckets
		String inventoryBucketName = "inventory";

		S3BucketDescriptor anotherBucket = new S3BucketDescriptor();
		anotherBucket.setName("antoherBucket");
		anotherBucket.setInventoryEnabled(true);

		when(mockConfig.getInventoryBucket()).thenReturn(inventoryBucketName);
		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(anotherBucket));

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			validator.validate();
		}).getMessage();

		assertEquals("An inventory bucket is defined but was not in the list of buckets.", errorMessage);
	}
}
