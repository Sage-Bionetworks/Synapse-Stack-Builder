package org.sagebionetworks.template.nlb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class RecordNameTest {

	@Test
	public void testConstructor() {
		RecordName domain = new RecordName("www.Synapse.Com");
		assertEquals("wwwsynapsecom", domain.getShortName());
		assertEquals("www-synapse-com", domain.getLongName());
	}
	
	@Test
	public void testConstructorNull() {
		assertThrows(IllegalArgumentException.class, ()->{
			new RecordName(null);
		});
	}
}
