package org.sagebionetworks.war;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Consumer;

import org.apache.logging.log4j.core.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class WarAppenderImplTest {
	
	@Mock
	Consumer<File> mockConsumer;
	
	@Mock
	LoggerFactory mockLoggerFactory;
	
	@Mock
	Logger mockLogger;
	
	WarAppenderImpl appender;
	
	@Captor
	ArgumentCaptor<File> directoryCapture;
	
	File testWar;
	
	@Before
	public void before() throws URISyntaxException {
		when(mockLoggerFactory.getLogger(any())).thenReturn(mockLogger);
		appender = new WarAppenderImpl(mockLoggerFactory);
		
		String testWarFileName = "test.war";
		URL url = WarAppenderImplTest.class.getClassLoader().getResource(testWarFileName);
		assertNotNull("Failed to find file on classpath: "+testWarFileName,url);
		testWar = new File(url.toURI());
	}
	
	@Test
	public void testAppendFilesCopyOfWar() {
		// call under test
		File copy = appender.appendFilesCopyOfWar(testWar, mockConsumer);
		try {
			assertNotNull(copy);
			assertTrue(copy.exists());
			verify(mockConsumer).accept(directoryCapture.capture());
			File tempDir = directoryCapture.getValue();
			assertNotNull(tempDir);
			assertFalse("The temp directory should no longer exist", tempDir.exists());
			verify(mockLogger, atLeast(2)).info(anyString());
		}finally {
			if (copy != null) {
				copy.delete();
			}
		}
	}

}
