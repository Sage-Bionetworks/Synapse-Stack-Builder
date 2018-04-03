package org.sagebionetworks.template;

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.junit.Test;

public class VpcTempalteTest {

	@Test
	public void testMergeTempalte() {
		VelocityContext context = new VelocityContext();
		List<String> colors = new LinkedList<>();
		colors.add("Red");
		colors.add("Blue");
		context.put("colors", colors);
		
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		ve.setProperty("runtime.references.strict", true);
		
		Template template = ve.getTemplate("templates/vpc/main-vpc-template.json");;
		StringWriter sw = new StringWriter();

		template.merge( context, sw);
		System.out.println(sw.toString());
	}
}
