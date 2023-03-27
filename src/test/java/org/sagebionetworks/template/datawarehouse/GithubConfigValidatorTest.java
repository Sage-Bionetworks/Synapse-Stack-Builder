package org.sagebionetworks.template.datawarehouse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GithubConfigValidatorTest {
    @InjectMocks
    private GithubConfigValidator githubConfigValidator;
    @Mock
    private GithubConfig githubConfig;

    private String basePath;
    private String filename;
    private String version;

    @BeforeEach
    public void before() {
        basePath = ("https://codeload.github.com/Sage-Bionetworks/Synapse-ETL-Jobs/zip/refs/tags");
        filename = "filename";
        version = "v1";
    }

    @Test
    public void testAllValidation() {
        when(githubConfig.getFilePaths()).thenReturn(Collections.singletonList(filename));
        when(githubConfig.getBasePath()).thenReturn(basePath);
        when(githubConfig.getVersion()).thenReturn(version);
        //call under test
        githubConfigValidator.validate();
        verify(githubConfig, times(1)).getFilePaths();
        verify(githubConfig, times(1)).getVersion();
        verify(githubConfig, times(1)).getBasePath();

    }

    @Test
    public void testGithubConfigWithOutFilename() {
        when(githubConfig.getBasePath()).thenReturn(basePath);
        when(githubConfig.getFilePaths()).thenReturn(Collections.EMPTY_LIST);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            githubConfigValidator.validate();
        }).getMessage();
        assertEquals("The github file path list cannot be empty", errorMessage);
    }

    @Test
    public void testGithubConfigWithOutVersion() {
        when(githubConfig.getFilePaths()).thenReturn(Collections.singletonList(filename));
        when(githubConfig.getBasePath()).thenReturn(basePath);
        when(githubConfig.getVersion()).thenReturn(null);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            githubConfigValidator.validate();
        }).getMessage();
        assertEquals("The github file version cannot be empty", errorMessage);
    }

    @Test
    public void testGithubConfigWithOutBasePath() {
        when(githubConfig.getBasePath()).thenReturn("");

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            githubConfigValidator.validate();
        }).getMessage();
        assertEquals("The github base path cannot be empty", errorMessage);
    }
}
