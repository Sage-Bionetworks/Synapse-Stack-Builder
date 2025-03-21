package org.sagebionetworks.template;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;


import java.util.LinkedList;
import java.util.List;

import software.amazon.awssdk.services.cloudformation.model.Tag;

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
		expectedTags.add(Tag.builder().key(TAG_KEY_DEPARTMENT).value(TAG_VALUE_DEPARTMENT).build());
		expectedTags.add(Tag.builder().key(TAG_KEY_PROJECT).value(TAG_VALUE_PROJECT).build());
		expectedTags.add(Tag.builder().key(TAG_KEY_OWNER_EMAIL).value(TAG_VALUE_OWNER_EMAIL).build());
		expectedTags.add(Tag.builder().key(TAG_KEY_EXECUTE_SCRIPT).value(TAG_VALUE_STACK_ARMOR).build());

		StackTagsProvider provider = new StackTagsProviderImpl();
		// call under test
		List<Tag> tags = provider.getStackTags();
		assertNotNull(tags);
		assertEquals(expectedTags.size(), tags.size());
		assertEquals(true, tags.containsAll(expectedTags));
	}

}