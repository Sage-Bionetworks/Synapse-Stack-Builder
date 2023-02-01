package org.sagebionetworks.template.nlb;

import java.util.Objects;

import org.sagebionetworks.template.repo.beanstalk.EnvironmentType;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Maps a DNS record name used to identify a static network load balancer (NLB)
 * to a dynamic application load balancer (ALB) target group identified by a
 * stack name. Example mapping input string: 'www.synapse.org->repo-prod-422-0'.
 *
 */
public class RecordToStackMapping {

	private static final String UNEXPECTED_MAPPING = "Unexpected mapping: '%s'.  Example mapping: 'www.synapse.org->repo-prod-422-0'";
	private static final String UNEXPECTED_TARGET = "Unexpected target: '%s'.  Example target: 'repo-prod-422-0'";

	private final String mapping;
	private final RecordName record;
	private final String target;
	private final RecordName dependsOn;

	private RecordToStackMapping(String mapping, String dependsOn) {
		ValidateArgument.required(mapping, "mapping");
		this.mapping = mapping;
		this.dependsOn = dependsOn != null? new RecordName(dependsOn): null;
		mapping = mapping.trim().toLowerCase();
		String[] split = mapping.split("->");
		if (split.length != 2) {
			throw new IllegalArgumentException(String.format(UNEXPECTED_MAPPING, mapping));
		}
		record = new RecordName(split[0]);

		String[] targetSplit = split[1].split("-");
		if (targetSplit.length != 4) {
			throw new IllegalArgumentException(String.format(UNEXPECTED_TARGET, split[1]));
		}
		if (!EnvironmentType.PORTAL.getShortName().equals(targetSplit[0])
				&& !EnvironmentType.REPOSITORY_SERVICES.getShortName().equals(targetSplit[0])) {
			throw new IllegalArgumentException(
					String.format("Found '%s' but expected 'portal' or 'reop'", targetSplit[0]));
		}
		if (!"prod".equals(targetSplit[1]) && !"dev".equals(targetSplit[1])) {
			throw new IllegalArgumentException(
					String.format("Found '%s' but expected 'prod' or 'dev'", targetSplit[1]));
		}
		try {
			Integer.parseInt(targetSplit[3]);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(String.format("Found '%s' but expected a number", targetSplit[3]));
		}
		EnvironmentType.valueOfPrefix(targetSplit[0]);
		target = split[1];
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		private String mapping;
		private String dependsOn;
		/**
		 * @param mapping the mapping to set
		 */
		public Builder withMapping(String mapping) {
			this.mapping = mapping;
			return this;
		}
		/**
		 * @param dependsOn the dependsOn to set
		 */
		public Builder withDependsOn(String dependsOn) {
			this.dependsOn = dependsOn;
			return this;
		}
		
		public RecordToStackMapping build() {
			return new RecordToStackMapping(mapping, dependsOn);
		}
		
	}

	/**
	 * @return the mapping
	 */
	public String getMapping() {
		return mapping;
	}

	/**
	 * @return the record
	 */
	public RecordName getRecord() {
		return record;
	}

	/**
	 * @return the target
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @return the dependsOn
	 */
	public RecordName getDependsOn() {
		return dependsOn;
	}

	@Override
	public int hashCode() {
		return Objects.hash(dependsOn, mapping, record, target);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof RecordToStackMapping)) {
			return false;
		}
		RecordToStackMapping other = (RecordToStackMapping) obj;
		return Objects.equals(dependsOn, other.dependsOn) && Objects.equals(mapping, other.mapping)
				&& Objects.equals(record, other.record) && Objects.equals(target, other.target);
	}

	@Override
	public String toString() {
		return "RecordToStackMapping [mapping=" + mapping + ", record=" + record + ", target=" + target + ", dependsOn="
				+ dependsOn + "]";
	}

}
