package org.sagebionetworks.template.dns;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AliasTargetDescriptorTest {

	@Test
	void testSetDnsNameNull() {
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AliasTargetDescriptor descriptor = new AliasTargetDescriptor();
			descriptor.setDnsName(null);
		});
		assertEquals("DNSName cannot be null", thrown.getMessage());
	}

	@Test
	void tetSetHostedZoneId() {
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AliasTargetDescriptor descriptor = new AliasTargetDescriptor();
			descriptor.setDnsName("name");
			descriptor.setHostedZoneId(null);
		});
		assertEquals("HostedZoneId cannot be null", thrown.getMessage());
	}

	@Test
	void testSetEvaluateTargetHealth() {
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AliasTargetDescriptor descriptor = new AliasTargetDescriptor();
			descriptor.setDnsName("name");
			descriptor.setHostedZoneId("zoneID");
			descriptor.setEvaluateTargetHealth(null);
		});
		assertEquals("EvaluateTargetHealth cannot be null", thrown.getMessage());
	}
}