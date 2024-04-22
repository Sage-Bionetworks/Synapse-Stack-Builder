package org.sagebionetworks.template.repo.appconfig;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.sagebionetworks.template.repo.queues.SnsAndSqsConfig;

@RunWith(MockitoJUnitRunner.class)
public class AppConfigVelocityContextProviderTest {
    @Mock
    VelocityContext mockContext;

    @Mock
    AppConfigConfig mockAppConfigConfig;

    @InjectMocks
    AppConfigVelocityContextProvider provider;

    List<AppConfigDescriptor> appConfigDescriptors;
    @Before
    public void setUp() throws IOException {
        appConfigDescriptors = new ArrayList<>();
        when(mockAppConfigConfig.getAppConfigConfigurations()).thenReturn(appConfigDescriptors);

    }
    @Test
    public void testAddToContext(){
        //method under test
        provider.addToContext(mockContext);

        verify(mockAppConfigConfig).getAppConfigConfigurations();
        verify(mockContext).put(APPCONFIG_CONFIGURATIONS, appConfigDescriptors);
    }
}
