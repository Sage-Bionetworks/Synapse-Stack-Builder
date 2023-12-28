package org.sagebionetworks.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.apache.http.client.HttpClient;
import org.junit.Test;
import org.sagebionetworks.template.utils.ArtifactDownload;
import org.sagebionetworks.template.utils.ArtifactDownloadImpl;

public class ArtifactDownloadImplTest {

    /**
     * This is an integration test for file download.
     */
    @Test
    public void testDownload() {
        // this is a small file
        String url = "https://sagebionetworks.jfrog.io/sagebionetworks/libs-releases-local/org/json/JSON-Java/maven-metadata.xml";
        HttpClient client = new TemplateGuiceModule().provideHttpClient();
        ArtifactDownload downloader = new ArtifactDownloadImpl(client);
        File temp = downloader.downloadFile(url);
        try {
            assertNotNull(temp);
            assertEquals(405L, temp.length());
        } finally {
            if (temp != null) {
                temp.delete();
            }
        }
    }
}
