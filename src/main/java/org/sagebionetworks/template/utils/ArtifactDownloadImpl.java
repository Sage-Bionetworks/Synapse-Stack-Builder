package org.sagebionetworks.template.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import com.google.inject.Inject;

public class ArtifactDownloadImpl implements ArtifactDownload {
    private HttpClient httpClient;

    @Inject
    public ArtifactDownloadImpl(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public File downloadFile(String url) {
        HttpGet httpget = new HttpGet(url);
        HttpResponse response;
        try {
            response = httpClient.execute(httpget);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException("Failed to download file: " + url + " Status code:"
                        + statusLine.getStatusCode() + " reason: " + statusLine.getReasonPhrase());
            }
            // download to a temp file.
            File temp = File.createTempFile("artifact", ".tmp");
            try (BufferedInputStream bis = new BufferedInputStream(response.getEntity().getContent());
                 BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(temp))) {
                int inByte;
                while ((inByte = bis.read()) != -1) {
                    bos.write(inByte);
                }
                bos.flush();
                return temp;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
