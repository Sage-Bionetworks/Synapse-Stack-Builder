package org.sagebionetworks.template.repo.appconfig;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class AppConfigConfigTest {
    @Test
    public void testConstructor_SnsNamesValidation(){
        AppConfigDescriptor appConfigDescriptor = new AppConfigDescriptor("", "", "");

        assertThrows(IllegalArgumentException.class, ()->{
            new AppConfigConfig(Arrays.asList(appConfigDescriptor));
        });
    }
}
