package org.sagebionetworks.template.repo.cloudwatchlogs;

import java.util.Objects;

public class LogDescriptor {
    private LogType logType;
    private String logPath;
    private String dateFormat;

    public LogType getLogType() { return logType; }
    public void setLogType(LogType logType) { this.logType = logType; }

    public String getLogPath() { return this.logPath; }
    public void setLogPath(String logPath) { this.logPath = logPath; }

    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogDescriptor that = (LogDescriptor) o;
        return logType.equals(that.logType) &&
                logPath.equals(that.logPath) &&
                dateFormat.equals(that.dateFormat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logType, logPath, dateFormat);
    }

}
