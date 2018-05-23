package org.sagebionetworks.template;

import java.util.Arrays;

import com.amazonaws.services.cloudformation.model.Parameter;

/**
 * Request to create or update a stack.
 *
 */
public class CreateOrUpdateStackRequest {

	String stackName;
	String templateBody;
	Parameter[] parameters;
	String[] capabilities;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(parameters);
		result = prime * result + ((stackName == null) ? 0 : stackName.hashCode());
		result = prime * result + ((templateBody == null) ? 0 : templateBody.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CreateOrUpdateStackRequest other = (CreateOrUpdateStackRequest) obj;
		if (!Arrays.equals(parameters, other.parameters))
			return false;
		if (stackName == null) {
			if (other.stackName != null)
				return false;
		} else if (!stackName.equals(other.stackName))
			return false;
		if (templateBody == null) {
			if (other.templateBody != null)
				return false;
		} else if (!templateBody.equals(other.templateBody))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CreateOrUpdateStackRequest [stackName=" + stackName + ", templateBody=" + templateBody + ", parameters="
				+ Arrays.toString(parameters) + "]";
	}

}
