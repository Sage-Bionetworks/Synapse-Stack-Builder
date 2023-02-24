package org.sagebionetworks.template.etl;

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

    private GithubPath githubPath;

    @BeforeEach
    public void before() {
        githubPath = new GithubPath();
        githubPath.setBasePath("https://basePath");
        githubPath.setFilePath("filePath");
        githubPath.setFilename("fileName");
        githubPath.setVersion("v1");
    }

    @Test
    public void testAllValidation() {
        when(githubConfig.getGithubPath()).thenReturn(Collections.singletonList(githubPath));

        //call under test
        githubConfigValidator.validate();
        verify(githubConfig, times(1)).getGithubPath();

    }

    @Test
    public void testGithubConfigWithOutFilename() {
        githubPath.setFilename(null);
        when(githubConfig.getGithubPath()).thenReturn(Collections.singletonList(githubPath));

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            githubConfigValidator.validate();
        }).getMessage();
        assertEquals("The github file name cannot be empty", errorMessage);
    }

    @Test
    public void testGithubConfigWithOutVersion() {
        githubPath.setVersion("");
        when(githubConfig.getGithubPath()).thenReturn(Collections.singletonList(githubPath));

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            githubConfigValidator.validate();
        }).getMessage();
        assertEquals("The github file version cannot be empty", errorMessage);
    }

    @Test
    public void testGithubConfigWithOutBasePath() {
        githubPath.setBasePath("");
        when(githubConfig.getGithubPath()).thenReturn(Collections.singletonList(githubPath));

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            githubConfigValidator.validate();
        }).getMessage();
        assertEquals("The github base path cannot be empty", errorMessage);
    }
    @Test
    public void testGithubConfigWithOutFilePath() {
        githubPath.setFilePath("");
        when(githubConfig.getGithubPath()).thenReturn(Collections.singletonList(githubPath));

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            githubConfigValidator.validate();
        }).getMessage();
        assertEquals("The github file path cannot be empty", errorMessage);
    }
}
