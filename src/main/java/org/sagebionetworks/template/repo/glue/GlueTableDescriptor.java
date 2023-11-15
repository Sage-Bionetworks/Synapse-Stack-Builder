package org.sagebionetworks.template.repo.glue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GlueTableDescriptor {

	private String name;
	private String description;
	private List<GlueColumn> columns = new ArrayList<>();
	private List<GlueColumn> partitionKeys = new ArrayList<>();
	private String inputFormat;
	private String location;

	// Additional custom parameters for the glue table
	private Map<String, String> parameters = null;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<GlueColumn> getColumns() {
		return columns;
	}

	public void setColumns(List<GlueColumn> columns) {
		this.columns = columns;
	}

	public List<GlueColumn> getPartitionKeys() {
		return partitionKeys;
	}

	public void setPartitionKeys(List<GlueColumn> partitionKeys) {
		this.partitionKeys = partitionKeys;
	}

	public String getInputFormat() {
		return inputFormat;
	}

	public void setInputFormat(String inputFormat) {
		this.inputFormat = inputFormat;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof GlueTableDescriptor)) {
			return false;
		}
		GlueTableDescriptor other = (GlueTableDescriptor) obj;
		return Objects.equals(columns, other.columns) && Objects.equals(description, other.description)
				&& Objects.equals(inputFormat, other.inputFormat) && Objects.equals(location, other.location)
				&& Objects.equals(name, other.name) && Objects.equals(parameters, other.parameters)
				&& Objects.equals(partitionKeys, other.partitionKeys);
	}

	@Override
	public int hashCode() {
		return Objects.hash(columns, description, inputFormat, location, name, parameters, partitionKeys);
	}
}