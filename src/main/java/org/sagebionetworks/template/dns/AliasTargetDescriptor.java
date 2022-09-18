package org.sagebionetworks.template.dns;

import java.util.Objects;

public class AliasTargetDescriptor {
	private String DnsName;
	private Boolean evaluateTargetHealth; // Should always be false for aliases
	private String hostedZoneId; // Z2FDTNDATAQYW2 for aliases

	public AliasTargetDescriptor() {}

	public AliasTargetDescriptor(String dnsName) {
		DnsName = dnsName;
		this.evaluateTargetHealth = false;
		this.hostedZoneId = "Z2FDTNDATAQYW2";
	}

	public AliasTargetDescriptor(String dnsName, Boolean evaluateTargetHealth, String hostedZoneId) {
		DnsName = dnsName;
		this.evaluateTargetHealth = evaluateTargetHealth;
		this.hostedZoneId = hostedZoneId;
	}

	public String getDnsName() {
		return DnsName;
	}

	public void setDnsName(String dnsName) {
		DnsName = dnsName;
	}

	public Boolean getEvaluateTargetHealth() {
		return evaluateTargetHealth;
	}

	public void setEvaluateTargetHealth(Boolean evaluateTargetHealth) {
		this.evaluateTargetHealth = evaluateTargetHealth;
	}

	public String getHostedZoneId() {
		return hostedZoneId;
	}

	public void setHostedZoneId(String hostedZoneId) {
		this.hostedZoneId = hostedZoneId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AliasTargetDescriptor that = (AliasTargetDescriptor) o;
		return DnsName.equals(that.DnsName) && evaluateTargetHealth.equals(that.evaluateTargetHealth) && hostedZoneId.equals(that.hostedZoneId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(DnsName, evaluateTargetHealth, hostedZoneId);
	}

	@Override
	public String toString() {
		return "AliasTargetDescriptor{" +
				"DnsName='" + DnsName + '\'' +
				", evaluateTargetHealth=" + evaluateTargetHealth +
				", hostedZoneId='" + hostedZoneId + '\'' +
				'}';
	}
}
