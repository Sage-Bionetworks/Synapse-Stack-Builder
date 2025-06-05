package org.sagebionetworks.template.cdn.webacl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class TemplateLoaderImplTest {

    private TemplateLoaderImpl templateLoader;
    
    @BeforeEach
    void setUp() {
        templateLoader = new TemplateLoaderImpl();
    }

    @Test
    void loadTemplate_ValidTemplate_LoadsSuccessfully() {
        String templatePath = "cdn/template-loader-test.txt";
        
        // call under test
        String result = templateLoader.loadTemplate(templatePath);

        assertNotNull(result);
    }

    @Test
    void loadTemplate_NonexistentTemplate_ThrowsIllegalStateException() {
        String nonExistentPath = "non-existent-template.txt";
        
        // Call under test
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> templateLoader.loadTemplate(nonExistentPath)
        );
        assertEquals("Template not found: " + nonExistentPath, exception.getMessage());
    }

}