package org.sagebionetworks.stack.utils;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.stack.util.EncryptionUtils;


public class EncryptUtilsTest {
	
	@Test
	public void testRoundTrip(){
		String encryptionKey = "abcdefghijklmnopqrstuvwxyz";
		String plainText = "Some secret message";
		String cipherText = EncryptionUtils.encryptString(encryptionKey, plainText);
		assertNotNull(cipherText);
		System.out.println("cipherText: "+cipherText);
		assertFalse("The cipher text is the same as the plain text!",plainText.equals(cipherText));
		String decrypted = EncryptionUtils.decryptString(encryptionKey, cipherText);
		assertNotNull(decrypted);
		System.out.println("decrypted: "+decrypted);
		assertEquals(plainText, decrypted);
	}

}
