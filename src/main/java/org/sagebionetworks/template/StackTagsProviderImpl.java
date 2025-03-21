package org.sagebionetworks.template;

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
import software.amazon.awssdk.services.cloudformation.model.Tag;

import com.google.inject.Inject;

public class StackTagsProviderImpl implements StackTagsProvider {

	@Inject
	public StackTagsProviderImpl() {}

	@Override
	public List<Tag> getStackTags() {
		List<Tag> tags = new LinkedList<>();
		tags.add(Tag.builder().key(TAG_KEY_DEPARTMENT).value(TAG_VALUE_DEPARTMENT).build());
		tags.add(Tag.builder().key(TAG_KEY_PROJECT).value(TAG_VALUE_PROJECT).build());
		tags.add(Tag.builder().key(TAG_KEY_OWNER_EMAIL).value(TAG_VALUE_OWNER_EMAIL).build());
		tags.add(Tag.builder().key(TAG_KEY_EXECUTE_SCRIPT).value(TAG_VALUE_STACK_ARMOR).build());
		return tags;
	}
}
