package org.sagebionetworks.stack.utils;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Test;
import org.sagebionetworks.stack.util.PropertyFilter;

/**
 * Test for the 
 * @author John
 *
 */
public class PropertyFilterTest {
	
	
	@Test
	public void testReplaces(){
		Properties props = new Properties();
		props.setProperty("key.a", "1");
		props.setProperty("key.b", "2");
		props.setProperty("key.c", "${key.b}+${key.a}");
		props.setProperty("key.d", "(${key.c})-${key.a}");
		// Replace the values.
		PropertyFilter.replaceAllRegularExp(props);
		assertEquals("1", props.get("key.a"));
		assertEquals("2", props.get("key.b"));
		assertEquals("2+1", props.get("key.c"));
		assertEquals("(2+1)-1", props.get("key.d"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSelfReferenceCycle(){
		Properties props = new Properties();
		props.setProperty("key.a", "1+${key.a}");
		// Replace the values.
		PropertyFilter.replaceAllRegularExp(props);
		assertEquals("1+${key.a}", props.get("key.a"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeepStackWithACycle(){
		Properties props = new Properties();
		props.setProperty("key.a", "${key.b}");
		props.setProperty("key.b", "${key.c}");
		props.setProperty("key.c", "${key.d}");
		props.setProperty("key.d", "${key.a}");
		// Replace the values.
		PropertyFilter.replaceAllRegularExp(props);
	}
	
	@Test
	public void testDeepStackWithNoCycles(){
		Properties props = new Properties();
		props.setProperty("key.a", "${key.b}+1");
		props.setProperty("key.b", "${key.c}+1");
		props.setProperty("key.c", "${key.d}+1");
		props.setProperty("key.d", "${key.f}+1");
		props.setProperty("key.f", "1");
		// Replace the values.
		PropertyFilter.replaceAllRegularExp(props);
		assertEquals("1+1+1+1+1", props.get("key.a"));
		assertEquals("1+1+1+1", props.get("key.b"));
		assertEquals("1+1+1", props.get("key.c"));
		assertEquals("1+1", props.get("key.d"));
		assertEquals("1", props.get("key.f"));
	}

}
