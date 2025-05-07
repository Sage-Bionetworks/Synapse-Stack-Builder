package org.sagebionetworks.template;

public interface SesClientWrapper {
    void setBounceNotificationTopic(String domain, String notificationTopicArn);

    void setComplaintNotificationTopic(String domain, String notificationTopicArn);
}
