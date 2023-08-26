package org.sagebionetworks.template.repo.kinesis.firehose;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GlueTableDescriptor {

	private String name;
	private Map<String, String> columns = new HashMap<>();
	private Map<String, String> partitionKeys = new HashMap<>();
	
	// Additional custom parameters for the glue table
	private Map<String, String> parameters = null;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getColumns() {
		return columns;
	}

	public void setColumns(Map<String, String> columns) {
		this.columns = columns;
	}

	public Map<String, String> getPartitionKeys() {
		return partitionKeys;
	}

	public void setPartitionKeys(Map<String, String> partitionKeys) {
		this.partitionKeys = partitionKeys;
	}
	
	public Map<String, String> getParameters() {
		return parameters;
	}
	
	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	@Override
	public int hashCode() {
		return Objects.hash(columns, name, parameters, partitionKeys);
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
		GlueTableDescriptor other = (GlueTableDescriptor) obj;
		return Objects.equals(columns, other.columns) && Objects.equals(name, other.name) && Objects.equals(parameters, other.parameters)
				&& Objects.equals(partitionKeys, other.partitionKeys);
	}

	

}
