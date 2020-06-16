package org.sagebionetworks.template.repo.cloudwatchlogs;

import java.util.Objects;

public class LogDescriptor {
    private LogType logType;
    private String logPath;
    private int retentionInDays = 30;
    private String dateFormat;
    private String multilineStartPattern;

    public LogType getLogType() { return logType; }
    public void setLogType(LogType logType) { this.logType = logType; }

    public String getLogPath() { return this.logPath; }
    public void setLogPath(String logPath) { this.logPath = logPath; }

    public int getRetentionInDays() { return retentionInDays; }
    public void setRetentionInDays(int retentionInDays) { this.retentionInDays = retentionInDays; }

    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }

    public String getMultilineStartPattern() { return multilineStartPattern; }
    public void setMultilineStartPattern(String multilineStartPattern) { this.multilineStartPattern = multilineStartPattern; }

    public String getLogGroupName(String beanstalkEnvName) { return formatLogGroupName(beanstalkEnvName); }

    public String getFileName() { return this.logPath; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogDescriptor that = (LogDescriptor) o;
        return retentionInDays == that.retentionInDays &&
                logType.equals(that.logType) &&
                logPath.equals(that.logPath) &&
                dateFormat.equals(that.dateFormat) &&
                multilineStartPattern.equals(that.multilineStartPattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logType, logPath, retentionInDays, dateFormat, multilineStartPattern);
    }

    public static final String LOG_GROUP_NAME_FORMAT = "/aws/elasticbeanstalk/%s/%s";
    private String formatLogGroupName(String beanstalkEnvName) {
        return String.format(LOG_GROUP_NAME_FORMAT.format(beanstalkEnvName, logPath));
    }
}
