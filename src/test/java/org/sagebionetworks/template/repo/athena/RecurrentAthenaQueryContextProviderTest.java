package org.sagebionetworks.template.repo.athena;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.ATHENA_QUERY_DESCRIPTORS;
import static org.sagebionetworks.template.repo.athena.RecurrentAthenaQueryConfigTest.query;

import java.util.Arrays;
import java.util.Collections;

import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RecurrentAthenaQueryContextProviderTest {
	
	@Mock
	private VelocityContext mockContext;
	
	@Mock
	private RecurrentAthenaQueryConfig mockConfig;
	
	@InjectMocks
	private RecurrentAthenaQueryContextProvider provider;
	
	@Test
	public void testAddToContextWithNoQueries() {
		// Call under test
		provider.addToContext(mockContext);
		
		verify(mockContext).put(ATHENA_QUERY_DESCRIPTORS, Collections.emptyList());
		
	}

	@Test
	public void testAddToContextWithEmptyQueries() {
		when(mockConfig.getQueries()).thenReturn(Collections.emptyList());
		
		// Call under test
		provider.addToContext(mockContext);
		
		verify(mockContext).put(ATHENA_QUERY_DESCRIPTORS, Collections.emptyList());
		
	}
	
	@Test
	public void testAddToContextWithQuery() {
		
		when(mockConfig.getQueries()).thenReturn(Arrays.asList(
			query("id", "query.sql", "* * * * *", "QUEUE")	
		));
		
		// Call under test
		provider.addToContext(mockContext);
		
		RecurrentAthenaQuery expected = query("id", "query.sql", "* * * * *", "QUEUE");
		expected.setQueryString("SELECT * FROM ${stack}${instance}table");
		expected.setDatabase(RecurrentAthenaQueryContextProvider.DEFAULT_DATABASE);
		
		verify(mockContext).put(ATHENA_QUERY_DESCRIPTORS, Arrays.asList(
			expected
		));
		
	}
	
	@Test
	public void testAddToContextWithQueryAndVariableSubstitution() {
		
		when(mockConfig.getQueries()).thenReturn(Arrays.asList(
			query("id", "query.sql", "* * * * *", "QUEUE")	
		));
		
		when(mockContext.get("stack")).thenReturn("Stack");
		when(mockContext.get("instance")).thenReturn("Instance");
		
		// Call under test
		provider.addToContext(mockContext);
		
		RecurrentAthenaQuery expected = query("id", "query.sql", "* * * * *", "QUEUE");
		expected.setQueryString("SELECT * FROM StackInstancetable");
		expected.setDatabase(RecurrentAthenaQueryContextProvider.DEFAULT_DATABASE);
		
		verify(mockContext).put(ATHENA_QUERY_DESCRIPTORS, Arrays.asList(
			expected
		));
		
	}
	
	@Test
	public void testAddToContextWithCustomDatabase() {
		
		RecurrentAthenaQuery query = query("id", "query.sql", "* * * * *", "QUEUE");
		query.setDatabase("CustomDB");
		
		when(mockConfig.getQueries()).thenReturn(Arrays.asList(
			query
		));
		
		// Call under test
		provider.addToContext(mockContext);
		
		RecurrentAthenaQuery expected = query("id", "query.sql", "* * * * *", "QUEUE");
		expected.setQueryString("SELECT * FROM ${stack}${instance}table");
		expected.setDatabase("CustomDB");
		
		verify(mockContext).put(ATHENA_QUERY_DESCRIPTORS, Arrays.asList(
			expected
		));
		
	}

}
