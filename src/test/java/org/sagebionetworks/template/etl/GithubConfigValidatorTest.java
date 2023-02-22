package org.sagebionetworks.template.etl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    public void testAllValidation() {
        when(githubConfig.getGithubPath()).thenReturn("https://somepath");
        when(githubConfig.getVersion()).thenReturn("v1");
        when(githubConfig.getFilename()).thenReturn("test.py");

        //call under test
        githubConfigValidator.validate();
        verify(githubConfig, times(1)).getFilename();
        verify(githubConfig, times(1)).getGithubPath();
        verify(githubConfig, times(1)).getVersion();
    }

    @Test
    public void testGithubConfigWithOutFilename() {
        when(githubConfig.getGithubPath()).thenReturn("https://somepath");
        when(githubConfig.getFilename()).thenReturn(null);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            githubConfigValidator.validate();
        }).getMessage();
        assertEquals("The github file name cannot be empty", errorMessage);
    }

    @Test
    public void testGithubConfigWithOutVersion() {
        when(githubConfig.getGithubPath()).thenReturn("https://somepath");
        when(githubConfig.getVersion()).thenReturn("");
        when(githubConfig.getFilename()).thenReturn("test.py");

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            githubConfigValidator.validate();
        }).getMessage();
        assertEquals("The github file version cannot be empty", errorMessage);
    }

    @Test
    public void testGithubConfigWithOutPath() {
        when(githubConfig.getGithubPath()).thenReturn("");

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            githubConfigValidator.validate();
        }).getMessage();
        assertEquals("The github path cannot be empty", errorMessage);
    }
}
