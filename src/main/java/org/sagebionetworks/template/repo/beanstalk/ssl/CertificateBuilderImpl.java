package org.sagebionetworks.template.repo.beanstalk.ssl;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.cert.Certificate;

import org.bouncycastle.crypto.prng.BasicEntropySourceProvider;
import org.bouncycastle.crypto.prng.EntropySourceProvider;
import org.sagebionetworks.template.Configuration;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;

import com.amazonaws.services.s3.AmazonS3;

public class CertificateBuilderImpl implements CertificateBuilder {

	private AmazonS3 s3Client;
	private Configuration configuration;

	@Override
	public CertificateInfo buildCertificate() {
		String bucket = configuration.getConfigurationBucket();

		return null;
	}

	public static KeyPair createNewKey(byte[] seed) throws NoSuchAlgorithmException, NoSuchProviderException {
		Provider provider = new BouncyCastleProvider();
		Security.addProvider(provider);
		Security.setProperty("crypto.policy", "unlimited");
		int maxKeySize = javax.crypto.Cipher.getMaxAllowedKeyLength("AES");
		if (maxKeySize < 256) {
			throw new IllegalStateException("Failed to set the crypo ploicy to unlimited");
		}
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(1024, new SecureRandom(seed));
		return keyGen.generateKeyPair();
	}

	public static void main(String[] args)
			throws NoSuchAlgorithmException, NoSuchProviderException, UnsupportedEncodingException {
		KeyPair key = createNewKey(args[0].getBytes("UTF-8"));
		PrivateKey priv = key.getPrivate();
		PublicKey pub = key.getPublic();
		String privateKey = new String(Base64.getEncoder().encode(priv.getEncoded()));
		String publicKey1 = new String(Base64.getEncoder().encode(pub.getEncoded()));
		String publicKey = new String(Base64.getEncoder().encode(publicKey1.getBytes()));
		System.out.println("Private key: ");
		System.out.println(privateKey);
		System.out.println("Public key1: ");
		System.out.println(publicKey1);
		System.out.println("Public key: ");
		System.out.println(publicKey);

        long now = System.currentTimeMillis();
        Date startDate = new Date(now);

        X500Name dnName = new X500Name(subjectDN);

        // Using the current timestamp as the certificate serial number
        BigInteger certSerialNumber = new BigInteger(Long.toString(now));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        // 1 Yr validity
        calendar.add(Calendar.YEAR, 1);

        Date endDate = calendar.getTime();

        // Use appropriate signature algorithm based on your keyPair algorithm.
        String signatureAlgorithm = "SHA256WithRSA";

        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair
                .getPublic().getEncoded());

        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(dnName,
                certSerialNumber, startDate, endDate, dnName, subjectPublicKeyInfo);

        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).setProvider(
                bcProvider).build(keyPair.getPrivate());

        X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

        Certificate selfSignedCert = new JcaX509CertificateConverter()
                .getCertificate(certificateHolder);

	}
}
