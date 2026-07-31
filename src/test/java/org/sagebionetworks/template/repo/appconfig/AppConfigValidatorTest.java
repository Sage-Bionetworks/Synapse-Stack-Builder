package org.sagebionetworks.template.repo.appconfig;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
public class AppConfigValidatorTest {
    @Mock
    private AppConfigConfig mockConfig;
    @InjectMocks
    private AppConfigConfigValidator validator;
    @Test
    public void testValidateConfigInputIsEmpty() {
        AppConfigConfig config = new AppConfigConfig();
        config.setAppConfigDescriptors(Collections.singletonList(new AppConfigDescriptor(
                "",
                "",
                null
        )));
        when(mockConfig.getAppConfigDescriptors()).thenReturn(config.getAppConfigDescriptors());
        String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            validator.validate();
        }).getMessage();
        assertEquals("The appConfig name is required and must not be the empty string.", errorMessage);
    }
    @Test
    public void testValidateConfigInvalidAppConfigName() {
        AppConfigConfig config = new AppConfigConfig();
        config.setAppConfigDescriptors(Collections.singletonList(new AppConfigDescriptor(
                "",
                "appConfigDescription",
                TextNode.valueOf("appConfigDefaultConfiguration")
        )));
        when(mockConfig.getAppConfigDescriptors()).thenReturn(config.getAppConfigDescriptors());
        String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            validator.validate();
        }).getMessage();
        assertEquals("The appConfig name is required and must not be the empty string.", errorMessage);
    }
    @Test
    public void testValidateConfigInvalidAppConfigDescription() {
        AppConfigConfig config = new AppConfigConfig();
        config.setAppConfigDescriptors(Collections.singletonList(new AppConfigDescriptor(
                "appConfigName",
                "",
                TextNode.valueOf("appConfigDefaultConfiguration")
        )));
        when(mockConfig.getAppConfigDescriptors()).thenReturn(config.getAppConfigDescriptors());
        String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            validator.validate();
        }).getMessage();
        assertEquals("The appConfig description is required and must not be the empty string.", errorMessage);
    }
    @Test
    public void testValidateConfigInvalidAppConfigDefaultConfiguration() {
        AppConfigConfig config = new AppConfigConfig();
        config.setAppConfigDescriptors(Collections.singletonList(new AppConfigDescriptor(
                "appConfigName",
                "appConfigDescription",
                null
        )));
        when(mockConfig.getAppConfigDescriptors()).thenReturn(config.getAppConfigDescriptors());
        String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            validator.validate();
        }).getMessage();
        assertEquals("The appConfig default configuration is required.", errorMessage);
    }
    @Test
    public void testValidateConfigInputIsValid() {
        AppConfigConfig config = new AppConfigConfig();
        config.setAppConfigDescriptors(Collections.singletonList(new AppConfigDescriptor(
                "appConfigName",
                "appConfigDescription",
                TextNode.valueOf("DefaultConfigurations")
        )));

        when(mockConfig.getAppConfigDescriptors()).thenReturn(config.getAppConfigDescriptors());
        validator.validate();
    }
}
