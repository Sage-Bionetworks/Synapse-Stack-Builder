package org.sagebionetworks.template.repo.appconfig;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


import java.util.List;
import org.junit.jupiter.api.Test;

public class AppConfigConfigTest {
    @Test
    public void testConstructorWithInvalidAppConfigName() {
        AppConfigDescriptor appConfigDescriptor = new AppConfigDescriptor("", "appConfigDescription", "appConfigDefaultConfiguration");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new AppConfigConfig(List.of(appConfigDescriptor));
        });
        assertTrue(exception.getMessage().contains("The appConfig name is required and must not be the empty string."));
    }
    @Test
    public void testConstructorWithInvalidAppConfigDescription() {
        AppConfigDescriptor appConfigDescriptor = new AppConfigDescriptor("appConfigName", "", "appConfigDefaultConfiguration");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new AppConfigConfig(List.of(appConfigDescriptor));
        });
        assertTrue(exception.getMessage().contains("The appConfig description is required and must not be the empty string."));
    }
    @Test
    public void testConstructorWithInvalidAppConfigDefaultConfiguration() {
        AppConfigDescriptor appConfigDescriptor = new AppConfigDescriptor("appConfigName", "appConfigDescription", "");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new AppConfigConfig(List.of(appConfigDescriptor));
        });
        assertTrue(exception.getMessage().contains("The appConfig default configuration is required and must not be the empty string."));
    }
}
