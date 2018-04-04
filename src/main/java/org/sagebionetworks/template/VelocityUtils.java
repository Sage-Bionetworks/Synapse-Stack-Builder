package org.sagebionetworks.template;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

public class VelocityUtils {

	/**
	 * Create a VelocityEngine configured to load resouces from the classpath.
	 * 
	 * @return
	 */
	public static VelocityEngine createEngine() {
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
		engine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		engine.setProperty("runtime.references.strict", true);
		return engine;
	}
}
