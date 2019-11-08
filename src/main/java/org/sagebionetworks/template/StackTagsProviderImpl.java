package org.sagebionetworks.template;

import com.amazonaws.services.cloudformation.model.Tag;
import com.google.inject.Inject;

import java.util.LinkedList;
import java.util.List;

import static org.sagebionetworks.template.Constants.*;
import static org.sagebionetworks.template.Constants.TAG_KEY_OWNER_EMAIL;

public class StackTagsProviderImpl implements StackTagsProvider {

	@Inject
	public StackTagsProviderImpl() {}

	@Override
	public List<Tag> getStackTags() {
		List<Tag> tags = new LinkedList<>();
		tags.add(new Tag().withKey(TAG_KEY_DEPARTMENT).withValue(TAG_VALUE_DEPARTMENT));
		tags.add(new Tag().withKey(TAG_KEY_PROJECT).withValue(TAG_VALUE_PROJECT));
		tags.add(new Tag().withKey(TAG_KEY_OWNER_EMAIL).withValue(TAG_VALUE_OWNER_EMAIL));
		return tags;
	}
}
