package org.sagebionetworks.template;

import software.amazon.awssdk.services.ses.model.SetIdentityNotificationTopicRequest;
import com.google.inject.Inject;

public class SesClientImpl implements SesClient {

    public static final String BOUNCE = "Bounce";
    public static final String COMPLAINT = "Complaint";
    software.amazon.awssdk.services.ses.SesClient sesClient;

    @Inject
    public SesClientImpl(software.amazon.awssdk.services.ses.SesClient sesClient) {
        super();
        this.sesClient = sesClient;
    }

    @Override
    public void setBounceNotificationTopic(String domain, String notificationTopicArn) {
        this.setSesNotificationTopic(domain, BOUNCE, notificationTopicArn);
    }

    @Override
    public void setComplaintNotificationTopic(String domain, String notificationTopicArn) {
        this.setSesNotificationTopic(domain, COMPLAINT, notificationTopicArn);
    }

    public void setSesNotificationTopic(String domain, String notificationType, String notificationTopicArn) {
        SetIdentityNotificationTopicRequest req = SetIdentityNotificationTopicRequest.builder()
                .identity(domain)
                .notificationType(notificationType)
                .snsTopic(notificationTopicArn)
                .build();
        this.sesClient.setIdentityNotificationTopic(req);
    }
}
