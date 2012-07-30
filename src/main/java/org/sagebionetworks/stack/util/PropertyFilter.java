package org.sagebionetworks.stack.util;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author John
 * 
 */
public class PropertyFilter {

	/**
	 * The replacement regular expression.
	 * 
	 */
	private static final String REG_EX = "(\\$\\{[^}]+\\})";
	
	/**
	 * The compiled pattern
	 */
	private static final Pattern PATTERN = Pattern.compile(REG_EX);

	/**
	 * Will replaces all values with ${...} regular expressions using the values
	 * of other properties.
	 * 
	 * @param toReplace
	 * @param replacments
	 */
	public static void replaceAllRegularExp(Properties toReplace) {
		// this set is used to keep track of the visited properties to detect
		// cycles.
		Set<String> visited = new HashSet<String>();
		Stack<String> stack = new Stack<String>();
		// Go through all properties
		for (Object keyOb : toReplace.keySet()) {
			String key = (String) keyOb;
			// Replace all keys as need
			recursiveReplaces(visited, stack, key, toReplace);
		}

	}

	/**
	 * Recursively replace the value of the given property key, using properties already defined.
	 * 
	 * @param visited - Used to only visit properties once.
	 * @param stack - Used to detect cycles.
	 * @param key - The current key to replace.
	 * @param toReplace - The properties we are working with.
	 */
	private static void recursiveReplaces(Set<String> visited, Stack<String> stack, String key,	Properties toReplace) {
		// If this key is already on the stack then we have a cycle.
		if(stack.contains(key)){
			throw new IllegalArgumentException(String.format("Cycle detected while filtering property values involving keys: %1$s, key: %2$s is already on the stack.", stack.toString(), key));
		}
		stack.push(key);
		// only visit a property once
		if(visited.add(key)){
			String value = toReplace.getProperty(key);
			if (value == null)	throw new IllegalArgumentException(String.format("Cannot replaces a property value using property key: %1$s because there is no proprety with that key",key));
			Matcher matcher = PATTERN.matcher(value);
			StringBuffer sb = new StringBuffer();
			// Find any matches
			while (matcher.find()) {
				String replacementKey = matcher.group(1);
				if (replacementKey != null) {
					replacementKey = replacementKey.substring(2,replacementKey.length() - 1);
					// Make sure the key we are about to use as a replacement has already been replaced itself.
					recursiveReplaces(visited, stack, replacementKey, toReplace);
					String replacmentValue = toReplace.getProperty(replacementKey);
					matcher.appendReplacement(sb, replacmentValue);
				}
			}
			matcher.appendTail(sb);
			toReplace.setProperty(key, sb.toString());
		}
		// pop this key off the stack.
		stack.pop();
	}

}
