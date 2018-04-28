package org.sagebionetworks.template.repo.beanstalk;

/**
 * AWS source bundle includes a S3 bucket and key.
 *
 */
public class SourceBundle {

	private String bucket;
	private String key;
	
	public SourceBundle(String bucket, String key) {
		super();
		this.bucket = bucket;
		this.key = key;
	}
	public String getBucket() {
		return bucket;
	}
	public String getKey() {
		return key;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SourceBundle other = (SourceBundle) obj;
		if (bucket == null) {
			if (other.bucket != null)
				return false;
		} else if (!bucket.equals(other.bucket))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "SourceBundle [bucket=" + bucket + ", key=" + key + "]";
	}
	
}
