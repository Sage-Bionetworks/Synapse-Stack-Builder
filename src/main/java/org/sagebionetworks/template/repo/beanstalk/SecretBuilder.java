package org.sagebionetworks.template.repo.beanstalk;

/**
 * Abstraction for creating secrets used to launch a stack.
 *
 */
public interface SecretBuilder {

	/**
	 * Build all of the secretes used to launch repo/workers.
	 * 
	 * @return
	 */
	public Secret[] createSecrets();

	/**
	 * Get the alias of the master key.
	 * 
	 * @return
	 */
	String getCMKAlias();

	/**
	 * Get the plaintext repository database password.
	 * 
	 * @return
	 */
	public String getRepositoryDatabasePassword();

	/**
	 * Get the plaintext ID generator database password.
	 * @return
	 */
	public String getIdGeneratorPassword();
}
