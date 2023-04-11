package org.sagebionetworks.template.datawarehouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class EtlJobConfig {
	private String githubRepo;
	private String version;
	private List<String> extraScripts;
    private List<EtlJobDescriptor> etlJobDescriptors = new ArrayList<>();
    
    public String getGithubRepo() {
		return githubRepo;
	}

	public EtlJobConfig withGithubRepo(String githubRepo) {
		this.githubRepo = githubRepo;
		return this;
	}

	public String getVersion() {
		return version;
	}

	public EtlJobConfig withVersion(String version) {
		this.version = version;
		return this;
	}

	public List<EtlJobDescriptor> getEtlJobDescriptors() {
        return etlJobDescriptors;
    }

    public EtlJobConfig withEtlJobDescriptors(List<EtlJobDescriptor> etlJobDescriptors) {
        this.etlJobDescriptors = etlJobDescriptors;
        return this;
    }
    
    public List<String> getExtraScripts() {
		return extraScripts;
	}
    
    public EtlJobConfig withExtraScripts(List<String> extraScripts) {
		this.extraScripts = extraScripts;
		return this;
	}

    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof EtlJobConfig)) {
			return false;
		}
		EtlJobConfig other = (EtlJobConfig) obj;
		return Objects.equals(etlJobDescriptors, other.etlJobDescriptors) && Objects.equals(extraScripts, other.extraScripts)
				&& Objects.equals(githubRepo, other.githubRepo) && Objects.equals(version, other.version);
	}

    @Override
	public int hashCode() {
		return Objects.hash(etlJobDescriptors, extraScripts, githubRepo, version);
	}
}
