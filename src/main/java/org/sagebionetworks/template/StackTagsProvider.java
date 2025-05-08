package org.sagebionetworks.template;

import java.util.List;

import org.sagebionetworks.template.config.Configuration;

import com.amazonaws.services.cloudformation.model.Tag;

public interface StackTagsProvider {

	public List<Tag> getStackTags(Configuration config);
}
