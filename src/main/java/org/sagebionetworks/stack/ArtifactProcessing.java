package org.sagebionetworks.stack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.sagebionetworks.stack.config.InputConfiguration;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationResult;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsResult;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.model.ProgressListener;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

/**
 * Downloads artifacts from Artifactory and uploads them as versions to S3.
 * @author John
 *
 */
public class ArtifactProcessing {

	private static Logger log = Logger.getLogger(ArtifactProcessing.class.getName());
	
	private static double BYTES_PER_MB = Math.pow(2,20);
	
	HttpClient httpClient;
	InputConfiguration config;
	AWSElasticBeanstalkClient beanstalkClient;
	AmazonS3Client s3Client;

	/**
	 * The IoC constructor.
	 * @param httpClient
	 */
	public ArtifactProcessing(HttpClient httpClient, AWSElasticBeanstalkClient beanstalkClient, AmazonS3Client s3Client, InputConfiguration config) {
		super();
		this.httpClient = httpClient;
		this.beanstalkClient = beanstalkClient;
		this.s3Client = s3Client;
		this.config = config;
	}


	/**
	 * Process all of the artifacts.
	 * 
	 * @throws IOException
	 */
	public void processArtifacts() throws IOException{
		// First create the application
		ApplicationDescription app = createApplication();
		
		// Create the application version for the portal
		createApplicationVersion(config.getPortalVersionLabel(), config.getPortalArtifactoryUrl());

	}
	
	/**
	 * Create the application if it does not exist
	 * @return
	 */
	public ApplicationDescription createApplication(){
		String appName = config.getElasticBeanstalkApplicationName();
		// Determine if the application already exists.
		DescribeApplicationsResult result = beanstalkClient.describeApplications(new DescribeApplicationsRequest().withApplicationNames(appName));
		if(result.getApplications().size() < 1){
			// Create the application
			beanstalkClient.createApplication(new CreateApplicationRequest(appName));
			// Query for it again.
			result = beanstalkClient.describeApplications(new DescribeApplicationsRequest().withApplicationNames(appName));
			if(result.getApplications().size() != 1){
				throw new IllegalArgumentException("Did not find one and only one Elastic Beanstalk Application with the name: "+appName);
			}
		}
		return result.getApplications().get(0);

	}
	
	/**
	 * Create the application version if it does not already exist.
	 * @param versionLabel
	 * @param fileURl
	 * @throws IOException
	 */
	public void createApplicationVersion(String versionLabel, String fileURL) throws IOException{
		// First determine if this version already exists
		log.debug(String.format("Creating version: %1$s using: %2$s ", versionLabel, fileURL));
		DescribeApplicationVersionsResult results = beanstalkClient.describeApplicationVersions(new DescribeApplicationVersionsRequest().withApplicationName(config.getElasticBeanstalkApplicationName()).withVersionLabels(versionLabel));
		if(results.getApplicationVersions().size() < 1){
			log.debug(String.format("Version: %1$s does not already existing so it will be created...", versionLabel));
			// first download the file
			// Download the artifacts
			File temp = null;
			String key = "versions/"+versionLabel;
			try{
				temp = downloadFile(fileURL);
				// Now upload it to s3.
				final long start = System.currentTimeMillis();;
				log.debug("Starting to upload file "+fileURL+" to S3...");

				PutObjectResult putResult = s3Client.putObject(new PutObjectRequest(config.getStackConfigS3BucketName(), key, temp).withProgressListener(new ProgressListener() {
					private volatile long lastUpdate = start;
					public void progressChanged(ProgressEvent progressEvent) {
						// They do not seem to know the meaning of the volatile key work at Amazon so the message never changes.
						// Therefore we just print a generic progress message.
						long now = System.currentTimeMillis();
						long lastUpdateElapase = now-lastUpdate;
						long totalElapse = now-start;
						if(lastUpdateElapase > 2*1000){
							// Log the event
							log.debug(String.format("Upload file to S3 progress event. Bytes transfered: %1$d, event code: %2$d.  Total upload elapse time: %3$d MS", progressEvent.getBytesTransfered(), progressEvent.getEventCode(), totalElapse));
							lastUpdate = now;
						}
					}
				}));
			}finally{
				if(temp != null){
					temp.delete();
				}
				// Clean up the file
			}
			// The S3 Location for this file.
			S3Location location = new S3Location(config.getStackConfigS3BucketName(), key);
			// we need to create this version
			beanstalkClient.createApplicationVersion(new CreateApplicationVersionRequest()
			.withApplicationName(config.getElasticBeanstalkApplicationName())
			.withAutoCreateApplication(false)
			.withSourceBundle(location)
			.withVersionLabel(versionLabel));
		}else{
			log.debug(String.format("Version: %1$s already exists.", versionLabel));
		}
	}

	/**
	 * Download the passed file URL to a temp file.
	 * @param fileUrl - File URL to download.
	 * @return - Resulting temp file.
	 * @throws IOException
	 */
	public File downloadFile(String fileUrl) throws IOException {
		log.debug("Downloading file...");
		log.debug(fileUrl);
		HttpGet get = new HttpGet(fileUrl);
		HttpResponse response = httpClient.execute(get);
		InputStream input = null;
		OutputStream output = null;
		byte[] buffer = new byte[1024];
		try {
			input = response.getEntity().getContent();
			File ouptFile = File.createTempFile("Artifact", ".tmp");
			output = new FileOutputStream(ouptFile);
			double bytes = 0.0;
			long start = System.currentTimeMillis();
			for (int length; (length = input.read(buffer)) > 0;) {
				output.write(buffer, 0, length);
				bytes += length;
				double downloadMB = bytes/BYTES_PER_MB;
				long now = System.currentTimeMillis();
				long elapse = now-start;
				// Record the size every 2 seconds.
				if(elapse > 2*1000){
					log.debug(String.format("Downloaded:  %5.2f MB", downloadMB));
					start = now;
				}
				// Be nice to the machine.
				Thread.yield();
			}
			double downloadMB = bytes/BYTES_PER_MB;
			log.debug(String.format("Finished downloaded:  %10.2f MB total", downloadMB));
			log.debug("Downloaded: "+fileUrl);
			log.debug("\t to temp file: "+ouptFile);
			return ouptFile;
		} finally {
			if (output != null){
				try {
					output.close();
				} catch (IOException e) {}
			}
			if (input != null){
				try {
					input.close();
				} catch (IOException e) {}
			}

		}
	}

}
