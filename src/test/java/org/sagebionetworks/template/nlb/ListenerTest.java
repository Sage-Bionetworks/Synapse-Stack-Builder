package org.sagebionetworks.template.nlb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ListenerTest {

	@Test
	public void testConstructorWithRepo80() {
		int port = 80;
		RecordToStackMapping mapping = RecordToStackMapping.builder().withMapping("prod.sagebase.org->repo-dev-123-5")
				.build();
		Listener listener = new Listener(port, mapping);
		assertEquals(port, listener.getPort());
		assertEquals("repo-dev-123-5", listener.getMapping().getTarget());
		assertEquals(port, listener.getHealthCheckPort());
		assertEquals("/repo/v1/version", listener.getHealthCheckPath());
		assertEquals("HTTP", listener.getHealthCheckProtocol());
	}
	
	@Test
	public void testConstructorWithRepo443() {
		int port = 443;
		RecordToStackMapping mapping = RecordToStackMapping.builder().withMapping("prod.sagebase.org->Repo-dev-123-5")
				.build();
		Listener listener = new Listener(port, mapping);
		assertEquals(port, listener.getPort());
		assertEquals("repo-dev-123-5", listener.getMapping().getTarget());
		assertEquals(port, listener.getHealthCheckPort());
		assertEquals("/repo/v1/version", listener.getHealthCheckPath());
		assertEquals("HTTPS", listener.getHealthCheckProtocol());
	}
	
	@Test
	public void testConstructorWithPortal80() {
		int port = 80;
		RecordToStackMapping mapping = RecordToStackMapping.builder().withMapping("prod.sagebase.org->portal-dev-123-5")
				.build();
		Listener listener = new Listener(port, mapping);
		assertEquals(port, listener.getPort());
		assertEquals("portal-dev-123-5", listener.getMapping().getTarget());
		assertEquals(port, listener.getHealthCheckPort());
		assertEquals("/", listener.getHealthCheckPath());
		assertEquals("HTTP", listener.getHealthCheckProtocol());
	}
	
	@Test
	public void testConstructorWithPortal443() {
		int port = 443;
		RecordToStackMapping mapping = RecordToStackMapping.builder().withMapping("prod.sagebase.org->portal-dev-123-5")
				.build();
		Listener listener = new Listener(port, mapping);
		assertEquals(port, listener.getPort());
		assertEquals("portal-dev-123-5", listener.getMapping().getTarget());
		assertEquals(port, listener.getHealthCheckPort());
		assertEquals("/", listener.getHealthCheckPath());
		assertEquals("HTTPS", listener.getHealthCheckProtocol());
	}
	
	@Test
	public void testConstructorWithNone80() {
		int port = 80;
		RecordToStackMapping mapping = RecordToStackMapping.builder().withMapping("prod.sagebase.org->none")
				.build();
		Listener listener = new Listener(port, mapping);
		assertEquals(port, listener.getPort());
		assertEquals(port, listener.getHealthCheckPort());
		assertEquals("/", listener.getHealthCheckPath());
		assertEquals("HTTP", listener.getHealthCheckProtocol());
	}
	
	@Test
	public void testConstructorWithNone443() {
		int port = 443;
		RecordToStackMapping mapping = RecordToStackMapping.builder().withMapping("prod.sagebase.org->none")
				.build();
		Listener listener = new Listener(port, mapping);
		assertEquals(port, listener.getPort());
		assertEquals(port, listener.getHealthCheckPort());
		assertEquals("/", listener.getHealthCheckPath());
		assertEquals("HTTPS", listener.getHealthCheckProtocol());
	}
}
