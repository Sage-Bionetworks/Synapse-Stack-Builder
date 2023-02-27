package org.sagebionetworks.template;

import org.apache.http.client.HttpClient;
import org.junit.Test;
import org.sagebionetworks.template.utils.ArtifactDownload;
import org.sagebionetworks.template.utils.ArtifactDownloadImpl;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
            assertEquals(376L, temp.length());
        } finally {
            if (temp != null) {
                temp.delete();
            }
        }
    }

    @Test
    public void testDownloadFileFromZip() {
        String url = "https://codeload.github.com/Sage-Bionetworks/Synapse-ETL-Jobs/zip/refs/tags/v0.1.0";
        HttpClient client = new TemplateGuiceModule().provideHttpClient();
        ArtifactDownload downloader = new ArtifactDownloadImpl(client);
        Map<String, File> fileMap = downloader.downloadFileFromZip(url, "v0.1.0",
                Collections.singleton("Synapse-ETL-Jobs-0.1.0/src/scripts/glue_jobs/process_access_record.py"));
        Map.Entry<String, File> entry = fileMap.entrySet().stream().findFirst().orElse(null);
        try {
            assertNotNull(entry);
            assertEquals("process_access_record_v0.1.0.py", entry.getKey());
            assertEquals(8466L, entry.getValue().length());
        } finally {
            entry.getValue().delete();
        }
    }
}
