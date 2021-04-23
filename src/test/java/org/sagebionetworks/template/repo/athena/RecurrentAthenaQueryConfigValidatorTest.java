package org.sagebionetworks.template.repo.athena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.repo.athena.RecurrentAthenaQueryConfigTest.query;
import static org.sagebionetworks.template.repo.athena.RecurrentAthenaQueryConfigTest.queue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.repo.queues.SnsAndSqsConfig;

@ExtendWith(MockitoExtension.class)
public class RecurrentAthenaQueryConfigValidatorTest {

	@Mock
	private RecurrentAthenaQueryConfig mockConfig;
	
	@Mock
	private SnsAndSqsConfig mockSqsConfig;
	
	@InjectMocks
	private RecurrentAthenaQueryConfigValidator validator;
	
	@Test
	public void testValidateWithNoQueries() {
		List<RecurrentAthenaQuery> queries = null;
		
		when(mockConfig.getQueries()).thenReturn(queries);
		
		// Call under test
		RecurrentAthenaQueryConfig result = validator.validate();
		
		assertEquals(mockConfig, result);
		
		verifyZeroInteractions(mockSqsConfig);
		
	}
	
	@Test
	public void testValidateWithEmptyQueries() {
		List<RecurrentAthenaQuery> queries = Collections.emptyList();
		
		when(mockConfig.getQueries()).thenReturn(queries);
		
		// Call under test
		RecurrentAthenaQueryConfig result = validator.validate();
		
		assertEquals(mockConfig, result);
		
		verifyZeroInteractions(mockSqsConfig);
	}
	
	@Test
	public void testValidateWithNoQueryName() {
		when(mockSqsConfig.getQueueDescriptors()).thenReturn(Arrays.asList(
			queue("QUEUE")
		));
		
		when(mockConfig.getQueries()).thenReturn(Arrays.asList(
			query(null, "query.sql", "* * * * *", "QUEUE")
		));
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			validator.validate();
		}).getMessage();
		
		assertEquals("The queryName is required.", message);
	}
	
	@Test
	public void testValidateWithNoQueryPath() {
		when(mockSqsConfig.getQueueDescriptors()).thenReturn(Arrays.asList(
			queue("QUEUE")
		));
		
		when(mockConfig.getQueries()).thenReturn(Arrays.asList(
			query("id", null, "* * * * *", "QUEUE")
		));
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			validator.validate();
		}).getMessage();
		
		assertEquals("The queryPath is required.", message);
	}
	
	@Test
	public void testValidateWithNoScheduleExpression() {
		when(mockSqsConfig.getQueueDescriptors()).thenReturn(Arrays.asList(
			queue("QUEUE")
		));
		
		when(mockConfig.getQueries()).thenReturn(Arrays.asList(
			query("id", "query.sql", null, "QUEUE")
		));
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			validator.validate();
		}).getMessage();
		
		assertEquals("The scheduleExpression is required.", message);
	}
	
	@Test
	public void testValidateWithNoDestinationQueue() {
		when(mockSqsConfig.getQueueDescriptors()).thenReturn(Arrays.asList(
			queue("QUEUE")
		));
		
		when(mockConfig.getQueries()).thenReturn(Arrays.asList(
			query("id", "query.sql", "* * * * *", null)
		));
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			validator.validate();
		}).getMessage();
		
		assertEquals("The destinationQueue is required.", message);
	}
		
	@Test
	public void testValidateWithDuplicateQueryName() {
		when(mockSqsConfig.getQueueDescriptors()).thenReturn(Arrays.asList(
			queue("QUEUE"),
			queue("QUEUE_2")
		));
		
		when(mockConfig.getQueries()).thenReturn(Arrays.asList(
			query("id", "query.sql", "* * * * *", "QUEUE"),
			query("id", "query.sql", "* * * * *", "QUEUE_2")
		));
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			validator.validate();
		}).getMessage();
		
		assertEquals("A query with name id was already defined", message);
	}
	
	@Test
	public void testValidateWithNonExistingQueueReference() {
		when(mockSqsConfig.getQueueDescriptors()).thenReturn(Arrays.asList(
			queue("QUEUE")
		));
		
		when(mockConfig.getQueries()).thenReturn(Arrays.asList(
			query("id", "query.sql", "* * * * *", "QUEUE"),
			query("id2", "query.sql", "* * * * *", "QUEUE_2")
		));
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			validator.validate();
		}).getMessage();
		
		assertEquals("The query with name id2 references the non defined queue QUEUE_2", message);
	}

}
