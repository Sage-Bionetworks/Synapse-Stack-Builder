package org.sagebionetworks.template.cdn.webacl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class TemplateLoaderImpl implements TemplateLoader {
    private static final Logger LOGGER = LogManager.getLogger(TemplateLoaderImpl.class);

    public String loadTemplate(String templatePath) {
        final ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(templatePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Template not found: " + templatePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load template: " + templatePath, e);
        }
    }
}