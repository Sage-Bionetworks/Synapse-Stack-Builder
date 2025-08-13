package org.sagebionetworks.template.utils;

import java.io.*;

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

    @Override
    public InputStream downloadAsStream(String url) {
        HttpGet httpget = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(httpget);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException("Failed to download file: " + url + " Status code:"
                        + statusLine.getStatusCode() + " reason: " + statusLine.getReasonPhrase());
            }

            return new BufferedInputStream(response.getEntity().getContent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] downloadAsBytes(String url) {
        try (InputStream inputStream = downloadAsStream(url)) {
            if (inputStream == null) {
                throw new IOException("Failed to open input stream from URL: " + url);
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int bytesRead;
            byte[] data = new byte[8192]; // 8KB buffer

            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }

            buffer.flush();
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
