package org.sagebionetworks.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TemplateUtilsTest {
	
	@Test
	public void testReplaceStackVariable() {
		String input = "${stack}.something";
		String stack = "prod";
		String expected = "prod.something";
		
		String result = TemplateUtils.replaceStackVariable(input, stack);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testReplaceStackVariableWithNullInput() {
		String input = null;
		String stack = "prod";
		
		String result = TemplateUtils.replaceStackVariable(input, stack);
		
		assertNull(result);
	}
	
	@Test
	public void testReplaceStackVariableWithUnreplaceable() {
		String input = "$${stack}.wrong";
		String stack = "prod";
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			TemplateUtils.replaceStackVariable(input, stack);
		}).getMessage();
		
		assertEquals("Unable to read input: $prod.wrong", errorMessage);
	}
	
}
