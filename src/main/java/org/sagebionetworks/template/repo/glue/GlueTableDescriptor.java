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

	public Map<String, String> getParameters() {
		return parameters;
	}
	
	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GlueTableDescriptor that = (GlueTableDescriptor) o;
		return name.equals(that.name) && Objects.equals(description, that.description) && columns.equals(that.columns)
				&& Objects.equals(partitionKeys, that.partitionKeys) && Objects.equals(parameters, that.parameters);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, description, columns, partitionKeys, parameters);
	}
}
