package org.sagebionetworks.template.repo.appconfig;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

public class AppConfigValidatorTest {

    @Test
    public void testValidateConfigInputIsEmpty() {
        AppConfigConfigValidator validator = new AppConfigConfigValidator();
        // Use assertThrows to check for IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                validator.validateConfigs(Collections.singletonList(new AppConfigDescriptor(
                        "",
                        "",
                        ""
                )))
        );
    }

    @Test
    public void testValidateConfigInputIsValid() {
        AppConfigConfigValidator validator = new AppConfigConfigValidator();
        // Corrected the way to handle list creation
        validator.validateConfigs(Collections.singletonList(new AppConfigDescriptor(
                "appConfigName",
                "appConfigDescription",
                "DefaultConfigurations"
        )));
    }
}
