package org.sagebionetworks.template;

import static org.sagebionetworks.template.Constants.PROPERTY_KEY_STACK;
import static org.sagebionetworks.template.Constants.TAG_KEY_DEPARTMENT;
import static org.sagebionetworks.template.Constants.TAG_KEY_EXECUTE_SCRIPT;
import static org.sagebionetworks.template.Constants.TAG_KEY_OWNER_EMAIL;
import static org.sagebionetworks.template.Constants.TAG_KEY_PROJECT;
import static org.sagebionetworks.template.Constants.TAG_VALUE_DEPARTMENT;
import static org.sagebionetworks.template.Constants.TAG_VALUE_OWNER_EMAIL;
import static org.sagebionetworks.template.Constants.TAG_VALUE_PROJECT;
import static org.sagebionetworks.template.Constants.TAG_VALUE_STACK_ARMOR;

import software.amazon.awssdk.services.cloudformation.model.Tag;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.template.config.Configuration;

import com.google.inject.Inject;

public class StackTagsProviderImpl implements StackTagsProvider {

	@Inject
	public StackTagsProviderImpl() {}

	@Override
	public List<Tag> getStackTags(Configuration config) {
		List<Tag> tags = new LinkedList<>();
		tags.add(Tag.builder().key(TAG_KEY_DEPARTMENT).value(TAG_VALUE_DEPARTMENT).build());
		tags.add(Tag.builder().key(TAG_KEY_PROJECT).value(TAG_VALUE_PROJECT).build());
		tags.add(Tag.builder().key(TAG_KEY_OWNER_EMAIL).value(TAG_VALUE_OWNER_EMAIL).build());
		// only add the 'install-stack-armor-agent' tag if the stack is the production stack
		if (Constants.isProd(config.getProperty(PROPERTY_KEY_STACK))) {
			tags.add(Tag.builder().key(TAG_KEY_EXECUTE_SCRIPT).value(TAG_VALUE_STACK_ARMOR).build());
		}
		return tags;
	}
}
