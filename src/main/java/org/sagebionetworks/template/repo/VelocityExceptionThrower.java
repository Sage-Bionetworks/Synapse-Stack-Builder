package org.sagebionetworks.template.repo;

public class VelocityExceptionThrower {

	public void throwException(String message) throws IllegalStateException {
		throw new IllegalStateException(message);
	}
	
}
