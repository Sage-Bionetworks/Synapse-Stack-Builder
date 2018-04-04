package org.sagebionetworks.stack.ssl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.sagebionetworks.stack.GeneratedResources;
import org.sagebionetworks.stack.ResourceProcessor;
import org.sagebionetworks.stack.StackEnvironmentType;

/**
 * Setup the SSL certificate
 * 
 * @author John
 *
 */
public class SSLSetup implements ResourceProcessor {
	
	private static Logger log = LogManager.getLogger(SSLSetup.class);
	
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
		this.setupSSLCertificate(StackEnvironmentType.REPO);
		this.setupSSLCertificate(StackEnvironmentType.WORKERS);
		this.setupSSLCertificate(StackEnvironmentType.PORTAL);
	}
	
	public void teardownResources() {
		throw new RuntimeException("Not implemented");
	}
	
	public void describeResources(String prefix) {
		describeSSLCertificate(StackEnvironmentType.REPO);
		describeSSLCertificate(StackEnvironmentType.WORKERS);
		describeSSLCertificate(StackEnvironmentType.PORTAL);
	}
	
	public void describeSSLCertificate(StackEnvironmentType env) {
		String certName = config.getSSLCertificateName(env);
		ServerCertificateMetadata meta = findCertificate(certName);
		if (meta == null) {
			throw new IllegalStateException("Failed to find or create the SSL certificate: " + config.getSSLCertificateName(env));
		} else {
			//config.setSSLCertificateARN(prefix, meta.getArn());
			resources.setSslCertificate(env, meta);
		}		
	}

	/**
	 * Setup the SSL certificate.
	 */
	public void setupSSLCertificate(StackEnvironmentType env){
		// First determine if the certificate already exists already exists
		ServerCertificateMetadata meta = findCertificate(config.getSSLCertificateName(env));
		if(meta == null){
			// Upload the parts of the certificate.
			UploadServerCertificateRequest request = new UploadServerCertificateRequest();
			request.setServerCertificateName(config.getSSLCertificateName(env));
			request.setPrivateKey(getCertificateStringFromS3(config.getSSlCertificatePrivateKeyName(env)));
			request.setCertificateBody(getCertificateStringFromS3(config.getSSLCertificateBodyKeyName(env)));
			request.setCertificateChain(getCertificateStringFromS3(config.getSSLCertificateChainKeyName(env)));
			UploadServerCertificateResult result = iamClient.uploadServerCertificate(request);
			log.debug("Created SSL certificate: "+result);
			// Search for it
			meta = findCertificate(config.getSSLCertificateName(env));
		}
		if(meta == null) throw new IllegalStateException("Failed to find or create the SSL certificate: "+config.getSSLCertificateName(env));
		// Also set the SSL Cert arn as a property
		//config.setSSLCertificateARN(prefix, meta.getArn());
		resources.setSslCertificate(env, meta);
	}

	/*
	 * Delete the SSL certificate
	 */
	public void deleteSSLCertificate(StackEnvironmentType env) {
		ServerCertificateMetadata meta = findCertificate(config.getSSLCertificateName(env));
		if (meta == null) {
			// Just log
			// TODO: Or throw IllegalStateException?
			log.debug("Could not find SSL certificate metadata for" + config.getSSLCertificateName(env));
		} else {
			DeleteServerCertificateRequest request = new DeleteServerCertificateRequest();
			request.setServerCertificateName(config.getSSLCertificateName(env));
			iamClient.deleteServerCertificate(request);
			meta = findCertificate(config.getSSLCertificateName(env));
		}
		if (meta != null) {
			throw new IllegalStateException("Failed to delete the SSL certificate: "+config.getSSLCertificateName(env));
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
                s3Object.getObjectContent().abort();
            throw new AmazonClientException("Unable to store object contents to disk: " + e.getMessage(), e);
        } finally {
            try {outputStream.close();} catch (Exception e) {}
            try {s3Object.getObjectContent().close();} catch (Exception e) {}
        }
	}
}
