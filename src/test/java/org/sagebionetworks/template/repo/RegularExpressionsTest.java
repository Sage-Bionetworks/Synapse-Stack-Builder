package org.sagebionetworks.template.repo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class RegularExpressionsTest {

	@Test
	public void testWebAclRegex() {

		VelocityContext context = new VelocityContext();

		// call under test
		RegularExpressions.bindRegexToContext(context);

		Set<String> expectedKeys = Arrays.stream(context.getKeys()).map(k -> k.toString())
				.filter(k -> k.startsWith("regex")).collect(Collectors.toSet());

		// All of the test cases are captured in this JSON file.
		JSONArray tests = new JSONObject(loadResource("web/acl/regex-test.json")).getJSONArray("tests");

		for (int i = 0; i < tests.length(); i++) {
			JSONObject test = tests.getJSONObject(i);
			String regexKey = test.getString("regexKey");
			expectedKeys.remove(regexKey);
			assertNotNull(regexKey, "missing regexKey");
			String rawRegex = (String) context.get(regexKey);
			assertNotNull(rawRegex, String.format("context missing regex for key '%s'", regexKey));
			String regex = RegularExpressions.fromEscapedJSON(rawRegex);
			Pattern p = Pattern.compile(regex);
			JSONArray hits = test.getJSONArray("hits");
			for (int h = 0; h < hits.length(); h++) {
				String hit = hits.getString(h);
				assertTrue(p.matcher(hit).matches(), String.format("Expected regex: '%s' to hit: '%s'", regex, hit));
			}
			JSONArray misses = test.getJSONArray("misses");
			for (int m = 0; m < misses.length(); m++) {
				String miss = misses.getString(m);
				assertFalse(p.matcher(miss).matches(), String.format("Expected regex: '%s' to mis: '%s'", regex, miss));
			}
		}
		expectedKeys.forEach(k -> assertTrue(false, String.format("No tests found for regex key: '%s'", k)));

	}

	public static String loadResource(String path) {
		try {
			try (InputStream in = RegularExpressionsTest.class.getClassLoader().getResourceAsStream(path)) {
				return IOUtils.toString(in, StandardCharsets.UTF_8);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
