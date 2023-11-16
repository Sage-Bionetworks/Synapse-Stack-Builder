package org.sagebionetworks.template.s3;

import java.util.Objects;

import com.amazonaws.services.s3.model.Permission;

public class S3BucketAclGrant {
	
	private String canonicalGrantee;
	private Permission permission;

	public S3BucketAclGrant() {}
	
	public String getCanonicalGrantee() {
		return canonicalGrantee;
	}
	
	public void setCanonicalGrantee(String canonicalGrantee) {
		this.canonicalGrantee = canonicalGrantee;
	}
	
	public Permission getPermission() {
		return permission;
	}
	
	public void setPermission(Permission permission) {
		this.permission = permission;
	}

	@Override
	public int hashCode() {
		return Objects.hash(canonicalGrantee, permission);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof S3BucketAclGrant)) {
			return false;
		}
		S3BucketAclGrant other = (S3BucketAclGrant) obj;
		return Objects.equals(canonicalGrantee, other.canonicalGrantee) && permission == other.permission;
	}

}
