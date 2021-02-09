package org.sagebionetworks.template;

public interface SesClient {
    void setBounceNotificationTopic(String domain, String notificationTopicArn);

    void setComplaintNotificationTopic(String domain, String notificationTopicArn);
}
