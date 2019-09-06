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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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

		StackTagsProvider provider = new StackTagsProviderImpl();
		// call under test
		List<Tag> tags = provider.getStackTags();
		assertNotNull(tags);
		assertEquals(3, tags.size());
		assertEquals(true, tags.containsAll(expectedTags));
	}

}