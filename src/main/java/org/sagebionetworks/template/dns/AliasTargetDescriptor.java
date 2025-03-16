package org.sagebionetworks.template.dns;

import software.amazon.awssdk.services.route53.model.AliasTarget;

import java.util.Objects;

public class AliasTargetDescriptor {
	private String dnsName;
	private Boolean evaluateTargetHealth; // Should always be false for aliases to Cloudfront distributions
	private String hostedZoneId; // Z2FDTNDATAQYW2 for aliases to Cloudfront distribution

	public AliasTargetDescriptor() {}
	public AliasTargetDescriptor(String dnsName, Boolean evaluateTargetHealth, String hostedZoneId) {
		this.dnsName = dnsName;
		this.evaluateTargetHealth = evaluateTargetHealth;
		this.hostedZoneId = hostedZoneId;
	}

	public AliasTargetDescriptor(AliasTarget target) {
		this.hostedZoneId = target.hostedZoneId();
		this.evaluateTargetHealth = target.evaluateTargetHealth();
		this.dnsName = target.dnsName();
	}

	public String getDnsName() {
		return dnsName;
	}

	public void setDnsName(String dnsName) { this.dnsName = dnsName; }

	public Boolean getEvaluateTargetHealth() {
		return evaluateTargetHealth;
	}
	public void setEvaluateTargetHealth(Boolean evaluateTargetHealth) { this.evaluateTargetHealth = evaluateTargetHealth; }

	public String getHostedZoneId() {
		return hostedZoneId;
	}
	public void setHostedZoneId(String hostedZoneId) { this.hostedZoneId = hostedZoneId; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AliasTargetDescriptor that = (AliasTargetDescriptor) o;
		return dnsName.equals(that.dnsName) && evaluateTargetHealth.equals(that.evaluateTargetHealth) && hostedZoneId.equals(that.hostedZoneId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dnsName, evaluateTargetHealth, hostedZoneId);
	}

	@Override
	public String toString() {
		return "AliasTargetDescriptor{" +
				"DnsName='" + dnsName + '\'' +
				", evaluateTargetHealth=" + evaluateTargetHealth +
				", hostedZoneId='" + hostedZoneId + '\'' +
				'}';
	}

	public AliasTarget toAliasTarget() {
		AliasTarget target = AliasTarget.builder()
				.dnsName(this.getDnsName())
				.hostedZoneId(this.getHostedZoneId())
				.evaluateTargetHealth(this.getEvaluateTargetHealth())
				.build();
		return target;
	}
}
