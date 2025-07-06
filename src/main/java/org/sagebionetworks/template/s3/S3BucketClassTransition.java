package org.sagebionetworks.template.s3;


import software.amazon.awssdk.services.s3.model.TransitionStorageClass;

import java.util.Objects;

/**
 * Transition rule for a bucket 
 */
public class S3BucketClassTransition {
	
	/**
	 * The target storage class
	 */
	private TransitionStorageClass storageClass;
	
	/**
	 * The number of days after object creation to transition the objects to
	 */
	private Integer days;
	
	public S3BucketClassTransition() { }

	public TransitionStorageClass getStorageClass() {
		return storageClass;
	}

	public S3BucketClassTransition withStorageClass(TransitionStorageClass storageClass) {
		this.storageClass = storageClass;
		return this;
	}

	public Integer getDays() {
		return days;
	}

	public S3BucketClassTransition withDays(Integer days) {
		this.days = days;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(days, storageClass);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		S3BucketClassTransition other = (S3BucketClassTransition) obj;
		return Objects.equals(days, other.days) && storageClass == other.storageClass;
	}

	@Override
	public String toString() {
		return "S3BucketClassTransition [storageClass=" + storageClass + ", days=" + days + "]";
	}

}
