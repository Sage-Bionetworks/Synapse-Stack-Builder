package org.sagebionetworks.template.repo.beanstalk.ssl;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 * Bouncy Castle implementation of a CertificateBuilder.
 * 
 * @author John
 *
 */
public class CertificateBuilderImpl implements CertificateBuilder {

	public static final String CERTIFICATE = "CERTIFICATE";
	public static final String RSA_PRIVATE_KEY = "RSA PRIVATE KEY";
	public static final String CERTIFICATE_DISTINGUISHED_NAME = "CN=SageBionetworks,O=SageBionetworks,L=Seattle,ST=Washington,C=US";
	/**
	 * See https://en.wikipedia.org/wiki/Key_size
	 */
	public static final int RSA_KEY_SIZE_BITS = 2048;

	@Override
	public CertificatePair buildNewX509CertificatePair() {
		try {
			// Create the key pair
			KeyPair keyPair = CertificateBuilderImpl.createNewKeyPair();
			// Create the X.509 public key certificate signed with the private key
			X509Certificate x509Certificate = CertificateBuilderImpl.generateX509Certificate(keyPair);
			// convert both to PEM.
			String privateKeyPEM = createPemString(RSA_PRIVATE_KEY, keyPair.getPrivate().getEncoded());
			String certificatePEM = createPemString(CERTIFICATE, x509Certificate.getEncoded());
			return new CertificatePair(certificatePEM, privateKeyPEM);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create the Privacy-Enhanced Mail (PEM) string for the given type and encoded
	 * bytes.
	 * 
	 * @param type
	 * @param encoded
	 * @return
	 * @throws IOException
	 */
	public static String createPemString(String type, byte[] encoded) throws IOException {
		StringWriter stringWriter = new StringWriter();
		try (PemWriter writer = new PemWriter(stringWriter)) {
			writer.writeObject(new PemObject(type, encoded));
		}
		return stringWriter.toString();
	}

	/**
	 * Create a new RAS KeyPair with key size of 2048 bits using
	 * {@link SecureRandom#getInstanceStrong()} as the entropy source. Uses bouncy
	 * castle.
	 * 
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 */
	public static KeyPair createNewKeyPair() throws NoSuchAlgorithmException {
		// Use the bouncy castle
		Security.addProvider(new BouncyCastleProvider());
		// Ensure we can create a key with a sufficient size.
		Security.setProperty("crypto.policy", "unlimited");
		String algorithm = "RSA";
		int maxKeySize = javax.crypto.Cipher.getMaxAllowedKeyLength(algorithm);
		if (maxKeySize < RSA_KEY_SIZE_BITS) {
			throw new IllegalStateException(
					"Cannot create a key with a sufficient number of bits. Max key size: " + maxKeySize);
		}
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(RSA_KEY_SIZE_BITS, new SecureRandom());
		return keyGen.generateKeyPair();
	}

	/**
	 * Generate a X.509 Certificate using the provided RSA key. The resulting
	 * certificate will be self-signed using "SHA256withRSA" and will expire in one
	 * year. Uses bouncy castle.
	 * 
	 * @param keyPair RSA public/private key pair.
	 * @return
	 * @throws IOException
	 * @throws OperatorCreationException
	 * @throws CertificateException
	 */
	public static X509Certificate generateX509Certificate(KeyPair keyPair)
			throws IOException, OperatorCreationException, CertificateException {
		// Use the bouncy castle
		Security.addProvider(new BouncyCastleProvider());
		// Valid between now and one year from now
		ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"));
		ZonedDateTime end = now.plusYears(1);
		Date startDate = Date.from(now.toInstant());
		Date endDate = Date.from(end.toInstant());

		// randomly generated serial number
		BigInteger serialNumber = new BigInteger(64, new SecureRandom());
		X500Name distinguishedName = new X500Name(CERTIFICATE_DISTINGUISHED_NAME);
		SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
		// start the builder with name, number, start, end, and public key.
		X509v3CertificateBuilder v3CertGen = new X509v3CertificateBuilder(distinguishedName, serialNumber, startDate,
				endDate, distinguishedName, subPubKeyInfo);
		// sign with the private key.
		String signingAlgorithm = "SHA256withRSA";
		AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signingAlgorithm);
		AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
		AsymmetricKeyParameter privateKeyAsymKeyParam = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
		ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(privateKeyAsymKeyParam);
		// build the certificate
		X509CertificateHolder certificateHolder = v3CertGen.build(sigGen);
		return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateHolder);
	}

}
