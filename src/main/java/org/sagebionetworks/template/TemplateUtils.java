package org.sagebionetworks.template;

import static org.sagebionetworks.template.Constants.STACK;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class TemplateUtils {
	
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
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

	public static <T> T loadFromJsonFile(String file, Class<T> clazz) throws IOException {
		return OBJECT_MAPPER.readValue(ClassLoader.getSystemClassLoader().getResource(file), clazz);
	}
	
	public static String loadContentFromFile(String file) {
		try(InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(file)) {
			if(in == null){
				throw new RuntimeException("Failed to load file content from the classpath: " + file);
			}
			return IOUtils.toString(in, StandardCharsets.UTF_8.name());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String prettyPrint(Object obj) throws IOException {
		OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
		return OBJECT_MAPPER.writeValueAsString(obj);
	}
	
}
