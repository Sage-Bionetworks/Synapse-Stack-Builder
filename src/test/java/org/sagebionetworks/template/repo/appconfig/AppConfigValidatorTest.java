package org.sagebionetworks.template.repo.appconfig;

import org.junit.Test;

public class AppConfigValidatorTest {
    @Test (expected = IllegalArgumentException.class)
    public void testValidateConfig_inputIsEmpty(){
        AppConfigConfigValidator.validateConfig(new AppConfigDescriptor(
                "",
                "",
                ""
        ));
    }
    @Test
    public void testValidateConfig_inputIsValid(){
        AppConfigConfigValidator.validateConfig(new AppConfigDescriptor(
                "appConfigName",
                "appConfigDescription",
                "DefaultConfigurations"
        ));
    }
}
