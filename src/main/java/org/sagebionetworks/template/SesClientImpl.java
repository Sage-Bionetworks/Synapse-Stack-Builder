package org.sagebionetworks.template;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SetIdentityNotificationTopicRequest;
import com.google.inject.Inject;
import org.sagebionetworks.template.config.Configuration;

public class SesClientImpl implements SesClient {

    public static final String BOUNCE = "Bounce";
    public static final String COMPLAINT = "Complaint";
    AmazonSimpleEmailService sesClient;

    @Inject
    public SesClientImpl(AmazonSimpleEmailService sesClient) {
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
        SetIdentityNotificationTopicRequest req = new SetIdentityNotificationTopicRequest()
                .withIdentity(domain)
                .withNotificationType(notificationType)
                .withSnsTopic(notificationTopicArn);
        this.sesClient.setIdentityNotificationTopic(req);
    }
}
