package org.sagebionetworks.template;

import com.amazonaws.services.cloudformation.model.Tag;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;


import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.sagebionetworks.template.Constants.*;

@RunWith(MockitoJUnitRunner.class)
public class StackTagsProviderTest {

	@Before
	public void before() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCreateStackTags() {
		List<Tag> expectedTags = new LinkedList<>();
		expectedTags.add(new Tag().withKey(TAG_KEY_DEPARTMENT).withValue(TAG_VALUE_DEPARTMENT));
		expectedTags.add(new Tag().withKey(TAG_KEY_PROJECT).withValue(TAG_VALUE_PROJECT));
		expectedTags.add(new Tag().withKey(TAG_KEY_OWNER_EMAIL).withValue(TAG_VALUE_OWNER_EMAIL));
		expectedTags.add(new Tag().withKey(TAG_KEY_EXECUTE_SCRIPT).withValue(TAG_VALUE_STACK_ARMOR));

		StackTagsProvider provider = new StackTagsProviderImpl();
		// call under test
		List<Tag> tags = provider.getStackTags();
		assertNotNull(tags);
		assertEquals(expectedTags.size(), tags.size());
		assertEquals(true, tags.containsAll(expectedTags));
	}

}