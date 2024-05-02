package org.sagebionetworks.template.repo.beanstalk;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

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
		descriptor.withSecretsSource(new SourceBundle("secretsBucket", "secretsKey"));
	}

	@Test
	public void testIsTypeRepositoryOrWorkers() {
		assertTrue(new EnvironmentDescriptor().withType(EnvironmentType.REPOSITORY_SERVICES).isTypeRepositoryOrWorkers());
		assertTrue(new EnvironmentDescriptor().withType(EnvironmentType.REPOSITORY_WORKERS).isTypeRepositoryOrWorkers());
		assertFalse(new EnvironmentDescriptor().withType(EnvironmentType.PORTAL).isTypeRepositoryOrWorkers());
	}
	@Test
	public void testIsTypePortal() {
		assertFalse(new EnvironmentDescriptor().withType(EnvironmentType.REPOSITORY_SERVICES).isTypePortal());
		assertFalse(new EnvironmentDescriptor().withType(EnvironmentType.REPOSITORY_WORKERS).isTypePortal());
		assertTrue(new EnvironmentDescriptor().withType(EnvironmentType.PORTAL).isTypePortal());
	}
}
