package org.sagebionetworks.stack;

/**
 * Information about a database connection.
 * 
 * @author John
 *
 */
public class DatabaseInfo {

	private String url;
	private String userName;
	private String plainTextPassword;
	private String encryptedPassword;
	
	public String getPlainTextPassword() {
		return plainTextPassword;
	}
	public void setPlainTextPassword(String plainTextPassword) {
		this.plainTextPassword = plainTextPassword;
	}
	/**
	 * The URL used to connect to this database.
	 * 
	 * @return
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * The URL used to connect to this database.
	 * @param url
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	
	/**
	 * The database user name
	 * @return
	 */
	public String getUserName() {
		return userName;
	}
	/**
	 * The database user name
	 * @param userName
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
	/**
	 * The encrypted password
	 * @return
	 */
	public String getEncryptedPassword() {
		return encryptedPassword;
	}
	/**
	 * The encrypted password
	 * @param encryptedPassword
	 */
	public void setEncryptedPassword(String encryptedPassword) {
		this.encryptedPassword = encryptedPassword;
	}
	
	
}
