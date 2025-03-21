package org.sagebionetworks.template;

import java.util.List;
import software.amazon.awssdk.services.cloudformation.model.Tag;

public interface StackTagsProvider {

	public List<Tag> getStackTags();
}
