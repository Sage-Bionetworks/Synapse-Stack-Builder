package org.sagebionetworks.template;

import com.google.inject.Inject;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SetIdentityNotificationTopicRequest;

public class SesClientWrapperImpl implements SesClientWrapper {

    public static final String BOUNCE = "Bounce";
    public static final String COMPLAINT = "Complaint";
    software.amazon.awssdk.services.ses.SesClient sesClient;

    @Inject
    public SesClientWrapperImpl(SesClient sesClient) {
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
