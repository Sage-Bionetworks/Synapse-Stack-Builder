package org.sagebionetworks.template.nlb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class RecordToStackMappingTest {

	@Test
	public void testBuild() {
		// call under test
		RecordToStackMapping mapping = RecordToStackMapping.builder()
				.withMapping(" www.Synapse.com->Portal-dev-123-4\t").build();
		assertNotNull(mapping.getRecord());
		assertEquals("www-synapse-com", mapping.getRecord().getLongName());
		assertEquals("wwwsynapsecom", mapping.getRecord().getShortName());
		assertEquals("portal-dev-123-4", mapping.getTarget());
		assertEquals(null, mapping.getDependsOn());
	}
	
	@Test
	public void testBuildWithDependsOn() {
		// call under test
		RecordToStackMapping mapping = RecordToStackMapping.builder()
				.withMapping(" www.Synapse.com->Portal-dev-123-4\t")
				.withDependsOn("other.org").build();
		assertNotNull(mapping.getRecord());
		assertNotNull(mapping.getDependsOn());
		assertEquals("other.org", mapping.getDependsOn().getName());
		assertEquals("www-synapse-com", mapping.getRecord().getLongName());
		assertEquals("www-synapse-com", mapping.getRecord().getLongName());
		assertEquals("wwwsynapsecom", mapping.getRecord().getShortName());
		assertEquals("portal-dev-123-4", mapping.getTarget());
	}

	@Test
	public void testBuildWithRepoProd() {
		// call under test
		RecordToStackMapping mapping = RecordToStackMapping.builder().withMapping(" www.Synapse.com->repo-prod-123-4\t")
				.build();
		assertNotNull(mapping.getRecord());
		assertEquals("www-synapse-com", mapping.getRecord().getLongName());
		assertEquals("wwwsynapsecom", mapping.getRecord().getShortName());
		assertEquals("repo-prod-123-4", mapping.getTarget());
	}

	@Test
	public void testConstructorWithNullMapping() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			RecordToStackMapping.builder().withMapping(null).build();
		}).getMessage();
		assertEquals("mapping is required.", message);
	}

	@Test
	public void testConstructorWithWrongDelimiter() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			RecordToStackMapping.builder().withMapping("www.Synapse.com=Portal-dev-123-4").build();
		}).getMessage();
		assertEquals(
				"Unexpected mapping: 'www.synapse.com=portal-dev-123-4'.  Example mapping: 'www.synapse.org->repo-prod-422-0'",
				message);
	}

	@Test
	public void testConstructorWithNoTarget() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			RecordToStackMapping.builder().withMapping("www.Synapse.com").build();
		}).getMessage();
		assertEquals("Unexpected mapping: 'www.synapse.com'.  Example mapping: 'www.synapse.org->repo-prod-422-0'",
				message);
	}

	@Test
	public void testConstructorWithNoRecord() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			RecordToStackMapping.builder().withMapping("Portal-dev-123-4").build();
		}).getMessage();
		assertEquals("Unexpected mapping: 'portal-dev-123-4'.  Example mapping: 'www.synapse.org->repo-prod-422-0'",
				message);
	}

	@Test
	public void testConstructorWithNoEnvironment() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			RecordToStackMapping.builder().withMapping("www.Synapse.com->dev-123-4").build();
		}).getMessage();
		assertEquals("Unexpected target: 'dev-123-4'.  Example target: 'repo-prod-422-0'", message);
	}

	@Test
	public void testConstructorWithWorker() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			RecordToStackMapping.builder().withMapping("www.Synapse.com->workers-dev-123-4").build();
		}).getMessage();
		assertEquals("Found 'workers' but expected 'portal' or 'reop'", message);
	}

	@Test
	public void testConstructorWithUnknownStack() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			RecordToStackMapping.builder().withMapping("www.Synapse.com->repo-wrong-123-4").build();
		}).getMessage();
		assertEquals("Found 'wrong' but expected 'prod' or 'dev'", message);
	}

	@Test
	public void testConstructorWithNotANumber() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			RecordToStackMapping.builder().withMapping("www.Synapse.com->repo-dev-123-foo").build();
		}).getMessage();
		assertEquals("Found 'foo' but expected a number", message);
	}

}
