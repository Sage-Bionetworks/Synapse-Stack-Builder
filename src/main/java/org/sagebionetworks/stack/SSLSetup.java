package org.sagebionetworks.stack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.print.attribute.standard.Fidelity;

import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.factory.AmazonClientFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.DeleteServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.ListServerCertificatesRequest;
import com.amazonaws.services.identitymanagement.model.ListServerCertificatesResult;
import com.amazonaws.services.identitymanagement.model.ServerCertificateMetadata;
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateResult;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

/**
 * Setup the SSL certificate
 * 
 * @author John
 *
 */
public class SSLSetup implements ResourceProcessor {
	
	private static Logger log = Logger.getLogger(SSLSetup.class);
	
	private AmazonIdentityManagementClient iamClient;
	private AmazonS3Client s3Client;
	private InputConfiguration config;
	private GeneratedResources resources;
	
	/**
	 * The IoC constructor.
	 * @param factory
	 * @param config
	 * @param resources
	 */
	public SSLSetup(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		super();
		this.initialize(factory, config, resources);
	}

	public void initialize(AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		this.iamClient = factory.createIdentityManagementClient();
		this.s3Client = factory.createS3Client();
		this.config = config;
		this.resources = resources;
	}
	
	public void setupResources() {
		this.setupSSLCertificate("plfm");
		this.setupSSLCertificate("worker");
		this.setupSSLCertificate("portal");
	}
	
	public void teardownResources() {
		throw new RuntimeException("Not implemented");
	}
	
	public void describeResources(String prefix) {
		describeSSLCertificate("plfm");
		describeSSLCertificate("worker");
		describeSSLCertificate("portal");
	}
	
	public void describeSSLCertificate(String prefix) {
		if (! (("plfm".equals(prefix)) || ("worker".equals(prefix)) || ("portal".equals(prefix)))) {
			throw new IllegalArgumentException("Allowed prefixes are 'plfm', 'worker', or 'portal'.");
		}
		String certName = config.getSSLCertificateName(prefix);
		ServerCertificateMetadata meta = findCertificate(certName);
		if (meta == null) {
			throw new IllegalStateException("Failed to find or create the SSL certificate: " + config.getSSLCertificateName(prefix));
		} else {
			//config.setSSLCertificateARN(prefix, meta.getArn());
			resources.setSslCertificate(prefix, meta);
		}		
	}

	/**
	 * Setup the SSL certificate.
	 */
	public void setupSSLCertificate(String prefix){
		if (! (("plfm".equals(prefix)) || ("worker".equals(prefix)) || ("portal".equals(prefix)))) {
			throw new IllegalArgumentException("Allowed prefixes are 'plfm', 'worker', or 'portal'.");
		}
		// First determine if the certificate already exists already exists
		ServerCertificateMetadata meta = findCertificate(config.getSSLCertificateName(prefix));
		if(meta == null){
			// Upload the parts of the certificate.
			UploadServerCertificateRequest request = new UploadServerCertificateRequest();
			request.setServerCertificateName(config.getSSLCertificateName(prefix));
			request.setPrivateKey(getCertificateStringFromS3(config.getSSlCertificatePrivateKeyName(prefix)));
			request.setCertificateBody(getCertificateStringFromS3(config.getSSLCertificateBodyKeyName(prefix)));
			request.setCertificateChain(getCertificateStringFromS3(config.getSSLCertificateChainKeyName(prefix)));
			UploadServerCertificateResult result = iamClient.uploadServerCertificate(request);
			log.debug("Created SSL certificate: "+result);
			// Search for it
			meta = findCertificate(config.getSSLCertificateName(prefix));
		}
		if(meta == null) throw new IllegalStateException("Failed to find or create the SSL certificate: "+config.getSSLCertificateName(prefix));
		// Also set the SSL Cert arn as a property
		//config.setSSLCertificateARN(prefix, meta.getArn());
		resources.setSslCertificate(prefix, meta);
	}

	/*
	 * Delete the SSL certificate
	 */
	public void deleteSSLCertificate(String prefix) {
		if (! (("plfm".equals(prefix)) || ("worker".equals(prefix)) || ("portal".equals(prefix)))) {
			throw new IllegalArgumentException("Allowed prefixes are 'plfm', 'worker', or 'portal'.");
		}
		ServerCertificateMetadata meta = findCertificate(config.getSSLCertificateName(prefix));
		if (meta == null) {
			// Just log
			// TODO: Or throw IllegalStateException?
			log.debug("Could not find SSL certificate metadata for" + config.getSSLCertificateName(prefix));
		} else {
			DeleteServerCertificateRequest request = new DeleteServerCertificateRequest();
			request.setServerCertificateName(config.getSSLCertificateName(prefix));
			iamClient.deleteServerCertificate(request);
			meta = findCertificate(config.getSSLCertificateName(prefix));
		}
		if (meta != null) {
			throw new IllegalStateException("Failed to delete the SSL certificate: "+config.getSSLCertificateName(prefix));
		}
	}
	/**
	 * Determine if the certificate already exists
	 * @param certName
	 * @return
	 */
	public ServerCertificateMetadata findCertificate(String certName){
		log.debug("Searching for Certificate: "+certName);
		// First we need to get all certificates
		List<ServerCertificateMetadata> allCerts = new LinkedList<ServerCertificateMetadata>();
		ListServerCertificatesResult results = iamClient.listServerCertificates();
		allCerts.addAll(results.getServerCertificateMetadataList());
		while(results.getMarker() != null){
			results = iamClient.listServerCertificates(new ListServerCertificatesRequest().withMarker(results.getMarker()));
			allCerts.addAll(results.getServerCertificateMetadataList());
		}
		// Check if our cert is in the list.
		for(ServerCertificateMetadata meta: allCerts){
			if(meta.getServerCertificateName().equals(certName)){
				log.debug("Certificate found: "+certName);
				return meta;
			}
		}
		// Did not find the cert.
		return null;
		
	}
	
	/**
	 * Download a certificate file from S3 directly into a string, skipping an intermediate file.
	 * @param key
	 * @return
	 */
	public String getCertificateStringFromS3(String key){
		// For this case we do not write to file first
		S3Object s3Object = s3Client.getObject(new GetObjectRequest(config.getDefaultS3BucketName(), key));
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024*10];
            int bytesRead;
            while ((bytesRead = s3Object.getObjectContent().read(buffer)) > -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            // Go right to string.
            return new String(outputStream.toByteArray(), "UTF-8");
        } catch (IOException e) {
            try {
                s3Object.getObjectContent().abort();
            } catch ( IOException abortException ) {
                log.warn("Couldn't abort stream", e);
            }
            throw new AmazonClientException("Unable to store object contents to disk: " + e.getMessage(), e);
        } finally {
            try {outputStream.close();} catch (Exception e) {}
            try {s3Object.getObjectContent().close();} catch (Exception e) {}
        }
	}
	
	
}
