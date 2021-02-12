package org.sagebionetworks.template.repo.beanstalk.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.junit.Test;

public class CertificateBuilderImplTest {

	@Test
	public void testCreatePemString() throws UnsupportedEncodingException, IOException {
		String toEncode = "Some string";
		String base64Encode = base64String(toEncode);
		// call under test
		String pem = CertificateBuilderImpl.createPemString("A TYPE", toEncode.getBytes("UTF-8"));
		assertTrue(pem.contains("A TYPE"));
		assertTrue(pem.contains(base64Encode));
	}
	
	/**
	 * Create a base64 encoded string.
	 * @param input
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	String base64String(String input) throws UnsupportedEncodingException {
		byte[] encoded = Base64.getEncoder().encode(input.getBytes("UTF-8"));
		return new String(encoded, "UTF-8");	
	}
	
	@Test
	public void testCreateNewKeyPair() throws NoSuchAlgorithmException, UnsupportedEncodingException {
		// call under test
		KeyPair pair = CertificateBuilderImpl.createNewKeyPair();
		assertNotNull(pair);
		// private key
		assertNotNull(pair.getPrivate());
		assertEquals("RSA", pair.getPrivate().getAlgorithm());
		assertEquals("PKCS#8", pair.getPrivate().getFormat());
		assertNotNull(pair.getPrivate().getEncoded());
		// public key
		assertNotNull(pair.getPublic());
		assertEquals("RSA",pair.getPublic().getAlgorithm());
		assertEquals("X.509", pair.getPublic().getFormat());
		// Size check
		BigInteger publicKeyValue = new BigInteger(pair.getPublic().getEncoded());
		System.out.println("Public key size: "+publicKeyValue.bitLength()+" bits");
		assertTrue(publicKeyValue.bitLength() > CertificateBuilderImpl.RSA_KEY_SIZE_BITS);
	}
	
	@Test
	public void testGenerateX509Certificate() throws Exception {
		KeyPair pair = CertificateBuilderImpl.createNewKeyPair();
		// call under test
		X509Certificate certificate = CertificateBuilderImpl.generateX509Certificate(pair);
//		System.out.println(certificate);
		assertNotNull(certificate);
		assertNotNull(certificate.getSubjectDN());
		assertEquals(CertificateBuilderImpl.CERTIFICATE_DISTINGUISHED_NAME, certificate.getSubjectDN().getName());
		// we are both the issuer and subject
		assertEquals(certificate.getIssuerDN().getName(), certificate.getSubjectDN().getName());
		assertNotNull(certificate.getNotBefore());
		assertNotNull(certificate.getNotAfter());
		// expire in one one year
		ZonedDateTime endDate = ZonedDateTime.ofInstant(certificate.getNotAfter().toInstant(), ZoneId.of("UTC"));
		ZonedDateTime startDate = ZonedDateTime.ofInstant(certificate.getNotBefore().toInstant(), ZoneId.of("UTC"));
		long months = ChronoUnit.MONTHS.between(startDate, endDate);
		assertTrue( months >= 12);
		assertNotNull(certificate.getSerialNumber());
		assertNotNull(certificate.getPublicKey());
		assertEquals("X.509", certificate.getPublicKey().getFormat());
		assertEquals("RSA", certificate.getPublicKey().getAlgorithm());
		assertEquals("SHA256WITHRSA", certificate.getSigAlgName());
	}

	@Test
	public void testBuildNewX509CertificatePair() {
		CertificateBuilderImpl builder = new CertificateBuilderImpl();
		// call under test
		CertificatePair pair = builder.buildNewX509CertificatePair();
		assertNotNull(pair);
		assertNotNull(pair.getPrivateKeyPEM());
		assertTrue(pair.getPrivateKeyPEM().contains(CertificateBuilderImpl.RSA_PRIVATE_KEY));
		assertNotNull(pair.getX509CertificatePEM());
		assertTrue(pair.getX509CertificatePEM().contains(CertificateBuilderImpl.CERTIFICATE));
	}
}
