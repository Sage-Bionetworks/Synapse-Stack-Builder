package org.sagebionetworks.template;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Tag;

/**
 * Request to create or update a stack.
 *
 */
public class CreateOrUpdateStackRequest {

	String stackName;
	String templateBody;
	Parameter[] parameters;
	String[] capabilities;
	private List<Tag> tags;
	private Boolean enableTerminationProtection;

	/**
	 * The name of the stack to create/update.
	 * 
	 * @return
	 */
	public String getStackName() {
		return stackName;
	}

	/**
	 * The name of the stack to create/update.
	 * 
	 * @param stackName
	 * @return
	 */
	public CreateOrUpdateStackRequest withStackName(String stackName) {
		this.stackName = stackName;
		return this;
	}

	/**
	 * The JSON template body for the stack.
	 * 
	 * @return
	 */
	public String getTemplateBody() {
		return templateBody;
	}

	/**
	 * The JSON template body for the stack.
	 * 
	 * @param templateBody
	 * @return
	 */
	public CreateOrUpdateStackRequest withTemplateBody(String templateBody) {
		this.templateBody = templateBody;
		return this;
	}

	/**
	 * The parameters passed to the stack being created or updated.
	 * 
	 * @return
	 */
	public Parameter[] getParameters() {
		return parameters;
	}

	/**
	 * The parameters passed to the stack being created or updated.
	 * 
	 * @param parameters
	 * @return
	 */
	public CreateOrUpdateStackRequest withParameters(Parameter... parameters) {
		this.parameters = parameters;
		return this;
	}
	
	/**
	 * Capabilities required to make IAM changes in cloud formation.
	 * 
	 * @param capabilities
	 * @return
	 */
	public CreateOrUpdateStackRequest withCapabilities(String...capabilities) {
		this.capabilities = capabilities;
		return this;
	}
	
	/**
	 * Capabilities required to make IAM changes in cloud formation.
	 * @return
	 */
	public String[] getCapabilities() {
		return capabilities;
	}

	/**
	 *	Tags for Cloudformation resources
	 * @return
	 * */
	public List<Tag> getTags() {
		return tags;
	}

	/**
	 * The list of tags passed to the stack being created or updated
	 *
	 * @param tags
	 */
	public CreateOrUpdateStackRequest withTags(List<Tag> tags) {
		this.tags = tags;
		return this;
	}

	/**
	 * Flag to enable termination protection
	 *
	 * @return
	 */
	public Boolean getEnableTerminationProtection() {
		return this.enableTerminationProtection;
	}

	/**
	 * Set enable termination protection on stack
	 *
	 * @param enableTerminationProtection
	 * @return
	 */
	public CreateOrUpdateStackRequest withEnableTerminationProtection(Boolean enableTerminationProtection) {
		this.enableTerminationProtection = enableTerminationProtection;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(capabilities);
		result = prime * result + Arrays.hashCode(parameters);
		result = prime * result + Objects.hash(enableTerminationProtection, stackName, tags, templateBody);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		CreateOrUpdateStackRequest other = (CreateOrUpdateStackRequest) obj;
		return Arrays.equals(capabilities, other.capabilities)
				&& Objects.equals(enableTerminationProtection, other.enableTerminationProtection)
				&& Arrays.equals(parameters, other.parameters) && Objects.equals(stackName, other.stackName)
				&& Objects.equals(tags, other.tags) && Objects.equals(templateBody, other.templateBody);
	}

	@Override
	public String toString() {
		return "CreateOrUpdateStackRequest [stackName=" + stackName + ", templateBody=" + templateBody + ", parameters="
				+ Arrays.toString(parameters) + ", capabilities=" + Arrays.toString(capabilities) + ", tags=" + tags
				+ ", enableTerminationProtection=" + enableTerminationProtection + "]";
	}

}
