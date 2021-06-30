package org.sagebionetworks.template.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.s3.model.StorageClass;

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
	
	@Test
	public void testValidateWithEmptyStorageClassTransitions() {
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("bucket");
		bucket.setStorageClassTransitions(Collections.emptyList());

		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(bucket));

		// Call under test
		validator.validate();
	}
	
	@Test
	public void testValidateWithStorageClassTransition() {
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("bucket");
		bucket.setStorageClassTransitions(Collections.singletonList(
				new S3BucketClassTransition()
					.withStorageClass(StorageClass.IntelligentTiering)
					.withDays(30)
		));

		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(bucket));

		// Call under test
		validator.validate();
	}
	
	@Test
	public void testValidateWithDuplicateStorageClassTransition() {
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("bucket");
		bucket.setStorageClassTransitions(Arrays.asList(
				new S3BucketClassTransition()
					.withStorageClass(StorageClass.IntelligentTiering)
					.withDays(30),
				new S3BucketClassTransition()
					.withStorageClass(StorageClass.IntelligentTiering)
					.withDays(10)
		));

		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(bucket));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			validator.validate();
		});
		
		assertEquals("Duplicate storageClass transition found for bucket bucket: S3BucketClassTransition [storageClass=INTELLIGENT_TIERING, days=10]", ex.getMessage());
	}
	
	@Test
	public void testValidateWithStorageClassTransitionAndNoStorageClass() {
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("bucket");
		bucket.setStorageClassTransitions(Arrays.asList(
				new S3BucketClassTransition()
					.withStorageClass(null)
					.withDays(30)
		));

		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(bucket));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			validator.validate();
		});
		
		assertEquals("The storageClass for the transition in bucket bucket is required.", ex.getMessage());
	}
	
	@Test
	public void testValidateWithStorageClassTransitionAndNoDays() {
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("bucket");
		bucket.setStorageClassTransitions(Arrays.asList(
				new S3BucketClassTransition()
					.withStorageClass(StorageClass.IntelligentTiering)
					.withDays(null)
		));

		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(bucket));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			validator.validate();
		});
		
		assertEquals("The days value must be greater than 0 for transition in bucket bucket", ex.getMessage());
	}
	
	@Test
	public void testValidateWithStorageClassTransitionAndDaysLessThanOne() {
		
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		bucket.setName("bucket");
		bucket.setStorageClassTransitions(Arrays.asList(
				new S3BucketClassTransition()
					.withStorageClass(StorageClass.IntelligentTiering)
					.withDays(0)
		));

		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(bucket));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			validator.validate();
		});
		
		assertEquals("The days value must be greater than 0 for transition in bucket bucket", ex.getMessage());
	}
	
	@Test
	public void testValidateWithIntArchiveConfiguration() {
		S3BucketDescriptor bucket = new S3BucketDescriptor();

		bucket.setName("bucket");
		bucket.setIntArchiveConfiguration(new S3IntArchiveConfiguration()
				.withArchiveAccessDays(90)
				.withDeepArchiveAccessDays(180)
				.withTagFilter(new S3TagFilter().withName("tag").withValue("test"))
		);

		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(bucket));
		
		validator.validate();
	}
	
	@Test
	public void testValidateWithIntArchiveConfigurationWithArchiveMinDays() {
		S3BucketDescriptor bucket = new S3BucketDescriptor();

		bucket.setName("bucket");
		bucket.setIntArchiveConfiguration(new S3IntArchiveConfiguration()
				.withArchiveAccessDays(60)
		);

		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(bucket));
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			validator.validate();
		});
		
		assertEquals("The minimum number of days for INT archive access is 90 days.", ex.getMessage());
	}
	
	@Test
	public void testValidateWithIntArchiveConfigurationWithDeepArchiveMinDays() {
		S3BucketDescriptor bucket = new S3BucketDescriptor();

		bucket.setName("bucket");
		bucket.setIntArchiveConfiguration(new S3IntArchiveConfiguration()
				.withDeepArchiveAccessDays(80)
		);

		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(bucket));
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			validator.validate();
		});
		
		assertEquals("The minimum number of days for INT deep archive access is 180 days.", ex.getMessage());
	}
	
	@Test
	public void testValidateWithIntArchiveConfigurationWithMissingTier() {
		S3BucketDescriptor bucket = new S3BucketDescriptor();

		bucket.setName("bucket");
		bucket.setIntArchiveConfiguration(new S3IntArchiveConfiguration());

		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(bucket));
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			validator.validate();
		});
		
		assertEquals("At least one of archive or deep archive number of days should be specified.", ex.getMessage());
	}
	
	@Test
	public void testValidateWithNotificationConfiguration() {
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		
		String topic = "topic";
		Set<String> events = new HashSet<>(Arrays.asList("s3:ObjectRestore:Completed", "s3:ObjectRestore:Post"));
		
		bucket.setName("bucket");
		bucket.setNotificationsConfiguration(new S3NotificationsConfiguration()
				.withTopic(topic)
				.WithEvents(events)
		);

		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(bucket));
		
		validator.validate();
	}
	
	@Test
	public void testValidateWithNotificationConfigurationAndNoTopic() {
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		
		String topic = "";
		Set<String> events = new HashSet<>(Arrays.asList("s3:ObjectRestore:Completed", "s3:ObjectRestore:Post"));
		
		bucket.setName("bucket");
		bucket.setNotificationsConfiguration(new S3NotificationsConfiguration()
				.withTopic(topic)
				.WithEvents(events)
		);

		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(bucket));
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			validator.validate();
		});
		
		assertEquals("The topic is required and must not be the empty string.", ex.getMessage());
	}
	
	@Test
	public void testValidateWithNotificationConfigurationAndNoEvent() {
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		
		String topic = "topic";
		Set<String> events = Collections.emptySet();
		
		bucket.setName("bucket");
		bucket.setNotificationsConfiguration(new S3NotificationsConfiguration()
				.withTopic(topic)
				.WithEvents(events)
		);

		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(bucket));
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			validator.validate();
		});
		
		assertEquals("The events is required and must not be empty.", ex.getMessage());
	}
	
	@Test
	public void testValidateWithNotificationConfigurationAndWrongEvent() {
		S3BucketDescriptor bucket = new S3BucketDescriptor();
		
		String topic = "topic";
		Set<String> events = Collections.singleton("someOtherEvent");
		
		bucket.setName("bucket");
		bucket.setNotificationsConfiguration(new S3NotificationsConfiguration()
				.withTopic(topic)
				.WithEvents(events)
		);

		when(mockConfig.getBuckets()).thenReturn(Arrays.asList(bucket));
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			validator.validate();
		});
		
		assertEquals("Unsupported event type: someOtherEvent", ex.getMessage());
	}
}
