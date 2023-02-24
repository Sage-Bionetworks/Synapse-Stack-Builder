package org.sagebionetworks.template;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PageIteratorTest {
	
	@Mock
	PageIterator.PageProvider<String> mockProvider;
	
	@Test
	public void testIteration() {
		
		when(mockProvider.nextPage()).thenReturn(List.of("a","b"), List.of("c"), Collections.emptyList());
		
		PageIterator<String> it = new PageIterator<>(mockProvider);
		
		// call under test
		List<String> results = new ArrayList<>();
		while(it.hasNext()) {
			results.add(it.next());
		}
		assertFalse(it.hasNext());
		
		List<String> expected = List.of("a","b","c");
		assertEquals(expected, results);
		
		verify(mockProvider, times(3)).nextPage();
	}

}
