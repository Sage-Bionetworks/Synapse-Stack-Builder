package org.sagebionetworks.template.utils;

import com.google.inject.Inject;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ArtifactDownloadImpl implements ArtifactDownload {
    private static final String EXTENSION = ".py";
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
    public Map<String, File> downloadFileFromZip(String url, String version, Set<String> filePaths) {
        HttpGet httpget = new HttpGet(url);
        HttpResponse response;
        try {
            response = httpClient.execute(httpget);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException("Failed to download file: " + url + " Status code:"
                        + statusLine.getStatusCode() + " reason: " + statusLine.getReasonPhrase());
            }

            Map<String, File> temFiles = new HashMap<>();

            try (ZipInputStream zipIn = new ZipInputStream(response.getEntity().getContent());
                 BufferedInputStream bis = new BufferedInputStream(zipIn);) {
                ZipEntry entry = zipIn.getNextEntry();

                while (entry != null) {
                    if (!entry.isDirectory() && filePaths.contains(entry.getName())) {
                        String entryName = entry.getName();
                        String fileName = entryName.substring(entryName.lastIndexOf("/") + 1,
                                entryName.lastIndexOf(".")) + "_" + version + EXTENSION;
                        File temp = File.createTempFile("github", ".tmp");
                        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(temp))) {
                            int inByte;
                            while ((inByte = bis.read()) != -1) {
                                bos.write(inByte);
                            }
                            temFiles.put(fileName, temp);
                            bos.flush();
                        }
                    }
                    zipIn.closeEntry();
                    entry = zipIn.getNextEntry();
                }
            }
            return temFiles;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
