package org.sagebionetworks.template.repo.cloudwatchlogs;

import java.util.Objects;

import org.sagebionetworks.template.repo.DeletionPolicy;

public class LogDescriptor {
    private LogType logType;
    private String logPath;
    private String dateFormat;
    private DeletionPolicy deletionPolicy;

    
    /**
	 * @return the deletionPolicy
	 */
	public String getDeletionPolicy() {
		return deletionPolicy.name();
	}
	/**
	 * @param deletionPolicy the deletionPolicy to set
	 */
	public void setDeletionPolicy(DeletionPolicy deletionPolicy) {
		this.deletionPolicy = deletionPolicy;
	}
	
	public LogType getLogType() { return logType; }
    public void setLogType(LogType logType) { this.logType = logType; }

    public String getLogPath() { return this.logPath; }
    public void setLogPath(String logPath) { this.logPath = logPath; }

    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }
    
	@Override
	public int hashCode() {
		return Objects.hash(dateFormat, deletionPolicy, logPath, logType);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof LogDescriptor)) {
			return false;
		}
		LogDescriptor other = (LogDescriptor) obj;
		return Objects.equals(dateFormat, other.dateFormat) && deletionPolicy == other.deletionPolicy
				&& Objects.equals(logPath, other.logPath) && logType == other.logType;
	}
}
