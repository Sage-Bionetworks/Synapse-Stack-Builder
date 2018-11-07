package org.sagebionetworks.war;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WarAppenderImplTest {
	
	@Mock
	Consumer<File> mockConsumer;
	
	@InjectMocks
	WarAppenderImpl appender;
	
	@Captor
	ArgumentCaptor<File> directoryCapture;
	
	File testWar;
	
	@Before
	public void before() throws URISyntaxException {
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
		}finally {
			if (copy != null) {
				copy.delete();
			}
		}
	}

}
