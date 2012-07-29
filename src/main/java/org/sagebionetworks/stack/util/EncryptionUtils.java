package org.sagebionetworks.stack.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * Simple utility for encrypting plain text and decrypting cipher text using DESede.
 * 
 * @author John
 *
 */
public class EncryptionUtils {
	
	private static final String UTF_8 = "UTF-8";
	public static final String DESEDE_ENCRYPTION_SCHEME = "DESede";
	
	/**
	 * Encrypt the plain text using the passed encryption key.
	 * 
	 * @param encryptionKey
	 * @param plainText
	 * @return The Base64 encoded DESede cipher text.
	 */
	public static String encryptString(String encryptionKey, String plainText) {
		if(encryptionKey == null) throw new IllegalArgumentException("Encryption key cannot be null");
		if(plainText == null) throw new IllegalArgumentException("Plain text cannot be null");
		try{
			DESedeKeySpec keySpec = new DESedeKeySpec(encryptionKey.getBytes(UTF_8));
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(DESEDE_ENCRYPTION_SCHEME);
			Cipher cipher = Cipher.getInstance(DESEDE_ENCRYPTION_SCHEME);
			SecretKey key = keyFactory.generateSecret(keySpec);
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] ciphertext = cipher.doFinal(plainText.getBytes(UTF_8));
			return new String(Base64.encodeBase64(ciphertext));
		}catch (Exception e){
			// Convert all errors to a runtime
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Decrypt the passed base64 encoded DESede cipher text using the passed encryption key.
	 * @param encryptionKey
	 * @param plainText
	 * @return The decrypted cipher text.
	 */
	public static String decryptString(String encryptionKey, String cipherText){
		if(encryptionKey == null) throw new IllegalArgumentException("Encryption key cannot be null");
		if(cipherText == null) throw new IllegalArgumentException("Cipher text cannot be null");
		try{
			DESedeKeySpec keySpec = new DESedeKeySpec(encryptionKey.getBytes(UTF_8));
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(DESEDE_ENCRYPTION_SCHEME);
			Cipher cipher = Cipher.getInstance(DESEDE_ENCRYPTION_SCHEME);
			SecretKey key = keyFactory.generateSecret(keySpec);
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] cipherBytes = Base64.decodeBase64(cipherText.getBytes(UTF_8));
			byte[] plainBytes = cipher.doFinal(cipherBytes);
			return new String(plainBytes, UTF_8);
		}catch (Exception e){
			// Convert all errors to a runtime
			throw new RuntimeException(e);
		}
	}

}
