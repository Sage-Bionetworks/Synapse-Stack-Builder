package org.sagebionetworks.template.s3;

import java.util.Objects;
import java.util.Set;

public class S3NotificationsConfiguration {
	
	private String topic;
	private Set<String> events;
	
	public String getTopic() {
		return topic;
	}
	public S3NotificationsConfiguration withTopic(String topic) {
		this.topic = topic;
		return this;
	}
	public Set<String> getEvents() {
		return events;
	}
	public S3NotificationsConfiguration WithEvents(Set<String> events) {
		this.events = events;
		return this;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(events, topic);
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
		S3NotificationsConfiguration other = (S3NotificationsConfiguration) obj;
		return Objects.equals(events, other.events) && Objects.equals(topic, other.topic);
	}
	
	@Override
	public String toString() {
		return "S3NotificationsConfiguration [topic=" + topic + ", events=" + events + "]";
	}

	

}
