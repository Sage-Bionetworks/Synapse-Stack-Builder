package org.sagebionetworks.template;

import com.amazonaws.services.cloudformation.model.Tag;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.config.Configuration;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.*;

@RunWith(MockitoJUnitRunner.class)
public class StackTagsProviderTest {

	@Mock
	Configuration mockConfig;

	@Before
	public void before() throws Exception {
		when(mockConfig.getProperty(PROPERTY_KEY_STACK_TAG_DEPARTMENT)).thenReturn("aDepartment");
		when(mockConfig.getProperty(PROPERTY_KEY_STACK_TAG_PROJECT)).thenReturn("aProject");
		when(mockConfig.getProperty(PROPERTY_KEY_STACK_TAG_OWNER_EMAIL)).thenReturn("anOwnerEmail");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCreateStackTags() {
		List<Tag> expectedTags = new LinkedList<>();
		expectedTags.add(new Tag().withKey(TAG_KEY_DEPARTMENT).withValue("aDepartment"));
		expectedTags.add(new Tag().withKey(TAG_KEY_PROJECT).withValue("aProject"));
		expectedTags.add(new Tag().withKey(TAG_KEY_OWNER_EMAIL).withValue("anOwnerEmail"));

		StackTagsProvider provider = new StackTagsProviderImpl(mockConfig);
		// call under test
		List<Tag> tags = provider.getStackTags();
		assertNotNull(tags);
		assertEquals(3, tags.size());
		assertEquals(true, tags.containsAll(expectedTags));
	}

}