package org.sagebionetworks.template;

import com.amazonaws.services.cloudformation.model.Tag;

import java.util.List;

public interface StackTagsProvider {

	public List<Tag> getStackTags();
}
