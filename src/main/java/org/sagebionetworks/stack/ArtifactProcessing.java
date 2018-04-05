package org.sagebionetworks.stack;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationRequest;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.sagebionetworks.stack.Constants.*;

import org.sagebionetworks.stack.config.InputConfiguration;
import org.sagebionetworks.stack.factory.AmazonClientFactory;


/**
 * Downloads artifacts from Artifactory and uploads them as versions to S3.
 * @author John
 *
 */
public class ArtifactProcessing {

	private static Logger log = LogManager.getLogger(ArtifactProcessing.class.getName());
	
	private static double BYTES_PER_MB = Math.pow(2,20);
	
	HttpClient httpClient;
	InputConfiguration config;
	AWSElasticBeanstalkClient beanstalkClient;
	AmazonS3Client s3Client;
	GeneratedResources resources;

	/**
	 * The IoC constructor.
	 * @param httpClient
	 * @param resources 
	 */
	public ArtifactProcessing(HttpClient httpClient, AmazonClientFactory factory, InputConfiguration config, GeneratedResources resources) {
		super();
		this.httpClient = httpClient;
		this.beanstalkClient = factory.createBeanstalkClient();
		this.s3Client = factory.createS3Client();
		this.config = config;
		this.resources = resources;
	}


	/**
	 * Process all of the artifacts.
	 * 
	 * @throws IOException
	 */
	public void processArtifacts() throws IOException{
		// First create the application
		resources.setElasticBeanstalkApplication(createOrGetApplication());
		
		// Create the application version for the portal
		resources.setPortalApplicationVersion(createOrGetApplicationVersion(PREFIX_PORTAL));
		// Create the application version for the reop
		resources.setRepoApplicationVersion(createOrGetApplicationVersion(PREFIX_REPO));
//		// Create the application version for the workers
		resources.setWorkersApplicationVersion(createOrGetApplicationVersion(PREFIX_WORKERS));
	}
	
	public void describeResources() {
		resources.setElasticBeanstalkApplication(describeApplication());
		resources.setPortalApplicationVersion(describeApplicationVersion(config.getVersionLabel(PREFIX_PORTAL)));
		resources.setRepoApplicationVersion(describeApplicationVersion(config.getVersionLabel(PREFIX_REPO)));
		resources.setWorkersApplicationVersion(describeApplicationVersion(config.getVersionLabel(PREFIX_WORKERS)));
	}
	
	/**
	 * Create the application if it does not exist
	 * @return
	 */
	public ApplicationDescription createOrGetApplication(){
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
	
	public ApplicationDescription describeApplication() {
		String appName = config.getElasticBeanstalkApplicationName();
		DescribeApplicationsResult result = beanstalkClient.describeApplications(new DescribeApplicationsRequest().withApplicationNames(appName));
		if (result.getApplications().size() != 1) {
			return null;
		} else {
			return result.getApplications().get(0);
		}
	}
	
	/**
	 * Create the application version if it does not already exist.
	 * @param versionLabel
	 * @param fileURl
	 * @throws IOException
	 */
	public ApplicationVersionDescription createOrGetApplicationVersion(String appPrfix) throws IOException{
		String s3Path = config.getVersionPath(appPrfix);
		final String versionLabel = config.getVersionLabel(appPrfix);
		String fileURL = config.getArtifactoryUrl(appPrfix);
		// First determine if this version already exists
		log.debug(String.format("Creating version: %1$s using: %2$s ", versionLabel, fileURL));
		DescribeApplicationVersionsResult results = beanstalkClient.describeApplicationVersions(new DescribeApplicationVersionsRequest().withApplicationName(config.getElasticBeanstalkApplicationName()).withVersionLabels(versionLabel));
		if(results.getApplicationVersions().size() < 1){
			log.debug(String.format("Version: %1$s does not already existing so it will be created...", versionLabel));
			// first download the file
			// Download the artifacts
			File temp = null;
			String key = s3Path+versionLabel;
			try{
				// First download the file from Artifactory
				temp = downloadFile(fileURL);
				// Now upload it to s3.
				final long start = System.currentTimeMillis();;
				log.debug("Starting to upload file "+fileURL+" to S3...");

				PutObjectResult putResult = s3Client.putObject(new PutObjectRequest(config.getStackConfigS3BucketName(), key, temp).withProgressListener(new ProgressListener() {
					private volatile long lastUpdate = start;
					public void progressChanged(ProgressEvent progressEvent) {
						// The progress data they give use never seems to change so we just show the elase time.
						long now = System.currentTimeMillis();
						long lastUpdateElapase = now-lastUpdate;
						long totalElapse = now-start;
						if(lastUpdateElapase > 2*1000){
							// Log the event
							log.debug(String.format("Uploading %1$s to S3. Elapse time: %2$tM:%2$tS:%2$tL ", versionLabel, totalElapse));
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
			// Describe the version
			results = beanstalkClient.describeApplicationVersions(new DescribeApplicationVersionsRequest().withApplicationName(config.getElasticBeanstalkApplicationName()).withVersionLabels(versionLabel));
		}else{
			log.debug(String.format("Version: %1$s already exists.", versionLabel));
		}
		return results.getApplicationVersions().get(0);
	}
	
	public ApplicationVersionDescription describeApplicationVersion(String versionLabel) {
		DescribeApplicationVersionsResult results = beanstalkClient.describeApplicationVersions(new DescribeApplicationVersionsRequest().withApplicationName(config.getElasticBeanstalkApplicationName()).withVersionLabels(versionLabel));
		if (results.getApplicationVersions().size() != 1) {
			return null;
		} else {
			return results.getApplicationVersions().get(0);
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
		if (response.getStatusLine().getStatusCode() == 404) {
			throw new IllegalArgumentException("Could not find " + fileUrl + " on Artifactory");
		}
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
					log.debug(String.format("Download progress: %1$5.2f MB for URL: %2$s",downloadMB, fileUrl));
					start = now;
				}
				// Be nice to the machine.
				Thread.yield();
			}
			if (bytes == 0.0) {
				throw new IOException("Downloaded artifact was 0 byte long.");
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
