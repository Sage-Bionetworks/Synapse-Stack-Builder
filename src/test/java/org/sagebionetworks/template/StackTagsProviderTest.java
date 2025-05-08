package org.sagebionetworks.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.template.Constants.DEV_STACK_NAME;
import static org.sagebionetworks.template.Constants.PROD_STACK_NAME;
import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.TAG_KEY_DEPARTMENT;
import static org.sagebionetworks.template.Constants.TAG_KEY_EXECUTE_SCRIPT;
import static org.sagebionetworks.template.Constants.TAG_KEY_OWNER_EMAIL;
import static org.sagebionetworks.template.Constants.TAG_KEY_PROJECT;
import static org.sagebionetworks.template.Constants.TAG_VALUE_DEPARTMENT;
import static org.sagebionetworks.template.Constants.TAG_VALUE_OWNER_EMAIL;
import static org.sagebionetworks.template.Constants.TAG_VALUE_PROJECT;
import static org.sagebionetworks.template.Constants.TAG_VALUE_STACK_ARMOR;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.template.config.Configuration;

import com.amazonaws.services.cloudformation.model.Tag;

@RunWith(MockitoJUnitRunner.class)
public class StackTagsProviderTest {
	@Mock
	Configuration mockConfig;
	   
	@Before
	public void before() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCreateStackTagsProd() {
		List<Tag> expectedTags = new LinkedList<>();
		expectedTags.add(new Tag().withKey(TAG_KEY_DEPARTMENT).withValue(TAG_VALUE_DEPARTMENT));
		expectedTags.add(new Tag().withKey(TAG_KEY_PROJECT).withValue(TAG_VALUE_PROJECT));
		expectedTags.add(new Tag().withKey(TAG_KEY_OWNER_EMAIL).withValue(TAG_VALUE_OWNER_EMAIL));
		expectedTags.add(new Tag().withKey(TAG_KEY_EXECUTE_SCRIPT).withValue(TAG_VALUE_STACK_ARMOR));
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(PROD_STACK_NAME);

		StackTagsProvider provider = new StackTagsProviderImpl();
		// call under test
		List<Tag> tags = provider.getStackTags(mockConfig);
		assertNotNull(tags);
		assertEquals(expectedTags.size(), tags.size());
		assertEquals(true, tags.containsAll(expectedTags));
	}

	@Test
	public void testCreateStackTagsNotProd() {
		List<Tag> expectedTags = new LinkedList<>();
		expectedTags.add(new Tag().withKey(TAG_KEY_DEPARTMENT).withValue(TAG_VALUE_DEPARTMENT));
		expectedTags.add(new Tag().withKey(TAG_KEY_PROJECT).withValue(TAG_VALUE_PROJECT));
		expectedTags.add(new Tag().withKey(TAG_KEY_OWNER_EMAIL).withValue(TAG_VALUE_OWNER_EMAIL));
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(DEV_STACK_NAME);

		StackTagsProvider provider = new StackTagsProviderImpl();
		// call under test
		List<Tag> tags = provider.getStackTags(mockConfig);
		assertNotNull(tags);
		assertEquals(expectedTags.size(), tags.size());
		assertEquals(true, tags.containsAll(expectedTags));
	}

}