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

    public String loadTemplate(String templatePath) {
        final ClassLoader classLoader = CdnWebAclBuilderImpl.class.getClassLoader();
        final Logger LOGGER = LogManager.getLogger(TemplateLoaderImpl.class);

        InputStream inputStream = classLoader.getResourceAsStream(templatePath);
        if (inputStream == null) {
            throw new IllegalStateException("Template not found: " + templatePath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load template: " + templatePath, e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                LOGGER.warn("Failed to close input stream for template: {}", templatePath, e);
            }
        }
    }
}
