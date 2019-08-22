package org.sagebionetworks.template;

import com.amazonaws.services.cloudformation.model.Tag;
import com.google.inject.Inject;
import org.sagebionetworks.template.config.Configuration;

import java.util.LinkedList;
import java.util.List;

import static org.sagebionetworks.template.Constants.*;
import static org.sagebionetworks.template.Constants.TAG_KEY_OWNER_EMAIL;

public class StackTagsProviderImpl implements StackTagsProvider {
	Configuration config;

	@Inject
	public StackTagsProviderImpl(Configuration config) {this.config=config;}

	@Override
	public List<Tag> getStackTags() {
		List<Tag> tags = new LinkedList<>();
		String department = config.getProperty(PROPERTY_KEY_STACK_TAG_DEPARTMENT);
		tags.add(new Tag().withKey(TAG_KEY_DEPARTMENT).withValue(department));
		String project = config.getProperty(PROPERTY_KEY_STACK_TAG_PROJECT);
		tags.add(new Tag().withKey(TAG_KEY_PROJECT).withValue(project));
		String ownerEmail = config.getProperty(PROPERTY_KEY_STACK_TAG_OWNER_EMAIL);
		tags.add(new Tag().withKey(TAG_KEY_OWNER_EMAIL).withValue(ownerEmail));
		return tags;
	}
}
