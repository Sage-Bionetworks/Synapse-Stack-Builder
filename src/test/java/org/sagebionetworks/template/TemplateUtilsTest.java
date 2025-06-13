package org.sagebionetworks.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentDescriptor;
import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;

import java.util.ArrayList;
import java.util.Collections;

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

	@Test
	public void testPrettyPrint() throws Exception{
		EnvironmentDescriptor descriptor = new EnvironmentDescriptor().withType(EnvironmentType.REPOSITORY_SERVICES).withName("aName").withImageId("imageId");
		String s = TemplateUtils.prettyPrint(descriptor);
		System.out.println(s);
	}

	@Test
	public void testPrettyPrintInvalid() throws Exception{
		EnvironmentDescriptor descriptor = new EnvironmentDescriptor().withName("aName").withImageId("imageId");
		String s = TemplateUtils.prettyPrint(descriptor);
		System.out.println(s);
	}


}
