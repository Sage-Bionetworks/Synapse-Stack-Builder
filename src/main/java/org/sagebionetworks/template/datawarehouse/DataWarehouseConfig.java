package org.sagebionetworks.template.datawarehouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.template.repo.glue.GlueTableDescriptor;


public class DataWarehouseConfig {
	private String githubRepo;
	private String version;
	private List<String> extraScripts;
	private List<GlueTableDescriptor> tableDescriptors = new ArrayList<>();
    private List<EtlJobDescriptor> etlJobDescriptors = new ArrayList<>();
    
    public String getGithubRepo() {
		return githubRepo;
	}

	public DataWarehouseConfig withGithubRepo(String githubRepo) {
		this.githubRepo = githubRepo;
		return this;
	}

	public String getVersion() {
		return version;
	}

	public DataWarehouseConfig withVersion(String version) {
		this.version = version;
		return this;
	}
	
	public List<GlueTableDescriptor> getTableDescriptors() {
		return tableDescriptors;
	}
	
	public DataWarehouseConfig withTableDescriptors(List<GlueTableDescriptor> tableDescriptors) {
		this.tableDescriptors = tableDescriptors;
		return this;
	}

	public List<EtlJobDescriptor> getEtlJobDescriptors() {
        return etlJobDescriptors;
    }

    public DataWarehouseConfig withEtlJobDescriptors(List<EtlJobDescriptor> etlJobDescriptors) {
        this.etlJobDescriptors = etlJobDescriptors;
        return this;
    }
    
    public List<String> getExtraScripts() {
		return extraScripts;
	}
    
    public DataWarehouseConfig withExtraScripts(List<String> extraScripts) {
		this.extraScripts = extraScripts;
		return this;
	}

    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DataWarehouseConfig)) {
			return false;
		}
		DataWarehouseConfig other = (DataWarehouseConfig) obj;
		return Objects.equals(etlJobDescriptors, other.etlJobDescriptors) && Objects.equals(extraScripts, other.extraScripts)
				&& Objects.equals(githubRepo, other.githubRepo) && Objects.equals(tableDescriptors, other.tableDescriptors)
				&& Objects.equals(version, other.version);
	}

    @Override
	public int hashCode() {
		return Objects.hash(etlJobDescriptors, extraScripts, githubRepo, tableDescriptors, version);
	}
}
