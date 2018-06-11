package org.sagebionetworks.template.repo.beanstalk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.cloudformation.model.Parameter;

public class EnvironmentDescriptorTest {
	
	EnvironmentDescriptor descriptor;
	
	Secret secret;
	
	@Before
	public void before() {
		secret = new Secret();
		secret.withEncryptedValue("EncryptedValue");
		secret.withParameterName("ParameterName");
		secret.withPropertyKey("PropertyKey");
		
		descriptor = new EnvironmentDescriptor();
		descriptor.withSecrets(new Secret[] {secret});
	}

	@Test
	public void testIsTypeRepositoryOrWorkers() {
		assertTrue(new EnvironmentDescriptor().withType(EnvironmentType.REPOSITORY_SERVICES).isTypeRepositoryOrWorkers());
		assertTrue(new EnvironmentDescriptor().withType(EnvironmentType.REPOSITORY_WORKERS).isTypeRepositoryOrWorkers());
		assertFalse(new EnvironmentDescriptor().withType(EnvironmentType.PORTAL).isTypeRepositoryOrWorkers());
	}
	
	@Test
	public void testCreateEnvironmentParameter() {
		// call under test
		Parameter parameter = EnvironmentDescriptor.createEnvironmentParameter(secret);
		assertNotNull(parameter);
		assertEquals(secret.getParameterName(), parameter.getParameterKey());
		assertEquals(secret.getEncryptedValue(), parameter.getParameterValue());
	}
	
	@Test
	public void testCreateEnvironmentParameters() {
		Parameter[] parameters = descriptor.createEnvironmentParameters();
		assertNotNull(parameters);
		assertEquals(1, parameters.length);
		assertEquals(secret.getParameterName(), parameters[0].getParameterKey());
		assertEquals(secret.getEncryptedValue(), parameters[0].getParameterValue());
	}
}
