package org.sagebionetworks.template;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.http.client.HttpClient;
import org.junit.Test;
import org.sagebionetworks.template.repo.beanstalk.ArtifactDownload;
import org.sagebionetworks.template.repo.beanstalk.ArtifactDownloadImpl;

public class ArtifactDownloadImplTest {
	
	/**
	 * This is an integration test for file download.
	 */
	@Test
	public void testDownload() {
		// this is a small file
		 String url = "http://sagebionetworks.artifactoryonline.com/sagebionetworks/libs-releases-local/org/json/JSON-Java/maven-metadata.xml";
		 HttpClient client = new TemplateGuiceModule().provideHttpClient();
		 ArtifactDownload downloader = new ArtifactDownloadImpl(client);
		 File temp = downloader.downloadFile(url);
		 try {
			 assertNotNull(temp);
			 assertEquals(376L, temp.length());
		 }finally {
			 if(temp != null) {
				 temp.delete();
			 }
		 }
	}

}
