package org.sagebionetworks.template.repo.appconfig;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AppConfigVelocityContextProviderTest {
    @Mock
    VelocityContext mockContext;

    @Mock
    AppConfigConfig mockAppConfigConfig;

    @InjectMocks
    AppConfigVelocityContextProvider provider;

    List<AppConfigDescriptor> appConfigDescriptors;
    @Test
    public void testAddToContext(){
        appConfigDescriptors = new ArrayList<>();
        when(mockAppConfigConfig.getAppConfigDescriptors()).thenReturn(appConfigDescriptors);
        //method under test
        provider.addToContext(mockContext);

        verify(mockAppConfigConfig).getAppConfigDescriptors();
        verify(mockContext).put(APPCONFIG_CONFIGURATIONS, appConfigDescriptors);
    }
}
