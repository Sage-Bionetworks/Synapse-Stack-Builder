package org.sagebionetworks.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.template.config.Configuration;
import software.amazon.awssdk.services.cloudformation.model.Tag;

@ExtendWith(MockitoExtension.class)
public class StackTagsProviderTest {
	@Mock
	Configuration mockConfig;

	@BeforeEach
	public void before() throws Exception {
	}

	@AfterEach
	public void tearDown() throws Exception {
	}

	@Test
	public void testCreateStackTagsProd() {
		List<Tag> expectedTags = new LinkedList<>();
		expectedTags.add(Tag.builder().key(TAG_KEY_DEPARTMENT).value(TAG_VALUE_DEPARTMENT).build());
		expectedTags.add(Tag.builder().key(TAG_KEY_PROJECT).value(TAG_VALUE_PROJECT).build());
		expectedTags.add(Tag.builder().key(TAG_KEY_OWNER_EMAIL).value(TAG_VALUE_OWNER_EMAIL).build());
		expectedTags.add(Tag.builder().key(TAG_KEY_EXECUTE_SCRIPT).value(TAG_VALUE_STACK_ARMOR).build());
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
		expectedTags.add(Tag.builder().key(TAG_KEY_DEPARTMENT).value(TAG_VALUE_DEPARTMENT).build());
		expectedTags.add(Tag.builder().key(TAG_KEY_PROJECT).value(TAG_VALUE_PROJECT).build());
		expectedTags.add(Tag.builder().key(TAG_KEY_OWNER_EMAIL).value(TAG_VALUE_OWNER_EMAIL).build());
		when(mockConfig.getProperty(PROPERTY_KEY_STACK)).thenReturn(DEV_STACK_NAME);

		StackTagsProvider provider = new StackTagsProviderImpl();
		// call under test
		List<Tag> tags = provider.getStackTags(mockConfig);
		assertNotNull(tags);
		assertEquals(expectedTags.size(), tags.size());
		assertEquals(true, tags.containsAll(expectedTags));
	}

}
