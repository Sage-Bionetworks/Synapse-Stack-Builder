package org.sagebionetworks.stack;

import java.io.IOException;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class ActivatorUtilsTest {
	
	public ActivatorUtilsTest() {
	}
	
	@Test
	public void testGetBackEndGenericCNAME() throws IOException {
		String expectedCNAME = "repo-prod.prod.sagebase.org";
		String cname = ActivatorUtils.getBackEndGenericCNAME("prod", "prod", "sagebase.org");
		assertEquals(expectedCNAME, cname);
	}
	
	@Test
	public void testGetBackEndInstanceCNAME() throws IOException {
		String expectedCNAME = "repo-prod-123-0.prod.sagebase.org";
		String cname = ActivatorUtils.getBackEndInstanceCNAME("prod", "123-0", "prod", "sagebase.org");
		assertEquals(expectedCNAME, cname);
	}
	
	@Test
	public void testGetProdPortalGenericCNAME() throws IOException {
		String expectedCNAME = "synapse-prod.synapse.org";
		String cname = ActivatorUtils.getPortalGenericCNAME("prod");
		assertEquals(expectedCNAME, cname);
	}
	
	@Test
	public void testGetStagingPortalGenericCNAME() throws IOException {
		String expectedCNAME = "synapse-staging.synapse.org";
		String cname = ActivatorUtils.getPortalGenericCNAME("staging");
		assertEquals(expectedCNAME, cname);
	}
	
	@Test
	public void testGetProdPortalInstanceCNAME() throws IOException {
		String expectedCNAME = "portal-prod-123-0.prod.sagebase.org";
		String cname = ActivatorUtils.getPortalInstanceCNAME("123-0", "prod", "sagebase.org");
		assertEquals(expectedCNAME, cname);
	}

	@Test
	public void testMapbackendGenericCNAMEToInstanceCNAME() throws IOException {
		Map<String, String> map = ActivatorUtils.mapBackendGenericCNAMEToInstanceCNAME("staging", "123-0");
		assertNotNull(map);
		assertEquals(1, map.size());
		assertTrue(map.containsKey("repo-staging.prod.sagebase.org"));
		String s = map.get("repo-staging.prod.sagebase.org");
		assertEquals("repo-prod-123-0.prod.sagebase.org", s);
		map = ActivatorUtils.mapBackendGenericCNAMEToInstanceCNAME("prod", "456-1");
		assertNotNull(map);
		assertEquals(1, map.size());
		assertTrue(map.containsKey("repo-prod.prod.sagebase.org"));
		s = map.get("repo-prod.prod.sagebase.org");
		assertEquals("repo-prod-456-1.prod.sagebase.org", s);
	}
	
	@Test
	public void testMapPortalGenericCNAMEToInstanceCNAME() throws IOException {
		Map<String, String> map = ActivatorUtils.mapPortalGenericCNAMEToInstanceCNAME("staging", "123-0");
		assertNotNull(map);
		assertEquals(1, map.size());
		assertTrue(map.containsKey("synapse-staging.synapse.org"));
		String s = map.get("synapse-staging.synapse.org");
		assertEquals("portal-prod-123-0.prod.sagebase.org", s);
		map = ActivatorUtils.mapPortalGenericCNAMEToInstanceCNAME("prod", "456-1");
		assertNotNull(map);
		assertEquals(1, map.size());
		assertTrue(map.containsKey("synapse-prod.synapse.org"));
		s = map.get("synapse-prod.synapse.org");
		assertEquals("portal-prod-456-1.prod.sagebase.org", s);
	}
	
}
