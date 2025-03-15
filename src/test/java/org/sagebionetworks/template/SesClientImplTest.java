package org.sagebionetworks.template;

import software.amazon.awssdk.services.ses.model.SetIdentityNotificationTopicRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SesClientImplTest {

    SesClientImpl client;

    @Mock
    software.amazon.awssdk.services.ses.SesClient mockAwsSesClient;

    @Captor
    ArgumentCaptor<SetIdentityNotificationTopicRequest> requestCaptor;

    @BeforeEach
    void setUp() {
        client = new SesClientImpl(mockAwsSesClient);
    }

    @Test
    void testSetBounceNotificationTopic() {
        // call under test
        client.setBounceNotificationTopic("myDomain", "myTopicArn");

        verify(mockAwsSesClient).setIdentityNotificationTopic(requestCaptor.capture());
        assertEquals("myTopicArn", requestCaptor.getValue().snsTopic());
        assertEquals("myDomain", requestCaptor.getValue().identity());
        assertEquals("Bounce", String.valueOf(requestCaptor.getValue().notificationType()));
    }

    @Test
    void testSetComplaintNotificationTopic() {
        // call under test
        client.setComplaintNotificationTopic("myDomain", "myTopicArn");

        verify(mockAwsSesClient).setIdentityNotificationTopic(requestCaptor.capture());
        assertEquals("myTopicArn", requestCaptor.getValue().snsTopic());
        assertEquals("myDomain", requestCaptor.getValue().identity());
        assertEquals("Complaint", String.valueOf(requestCaptor.getValue().notificationType()));
    }
}