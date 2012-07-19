package org.sagebionetworks.stack;

/**
 * Basic constants for building the stack.
 * 
 * @author John
 *
 */
public class Constants {
	
	/**
	 * Property key for the AWS ACCESS_KEY
	 */
	public static String AWS_ACCESS_KEY = "org.sagebionetworks.aws.access.key";
	/**
	 * Property key for the AWS SECRET_KEY
	 */
	public static String AWS_SECRET_KEY = "org.sagebionetworks.aws.secret.key";
	
	/**
	 * Property key for the encryption key used by this stack
	 */
	public static final String STACK_ENCRYPTION_KEY	 = "org.sagebionetworks.encryption.key";
	
	/**
	 * Property key for the stack.
	 */
	public static final String STACK = "org.sagebionetworks.stack";

	/**
	 * Property key for the stack instance.
	 */
	public static final String INSTANCE = "org.sagebionetworks.stack.instance";
	
	/**
	 * Property key for the default id generator password.
	 */
	public static final String KEY_DEFAULT_ID_GEN_PASSWORD = "org.sagebionetworks.id.generator.db.default.password";
}
