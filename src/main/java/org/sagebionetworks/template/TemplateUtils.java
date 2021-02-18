package org.sagebionetworks.template;

import static org.sagebionetworks.template.Constants.STACK;

public class TemplateUtils {
	
	public static String replaceStackVariable(String input, String stack) {
		if (input == null) {
			return null;
		}
		
		input = input.replace("${" + STACK + "}", stack);
		
		if(input.contains("$")) {
			throw new IllegalArgumentException("Unable to read input: " + input);
		}
		
		return input;
	}

}
