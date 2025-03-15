package org.sagebionetworks.template;

import software.amazon.awssdk.services.ses.model.SetIdentityNotificationTopicRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SesClientImplTest {

    SesClientImpl client;

    @Mock
    software.amazon.awssdk.services.ses.SesClient mockAwsSesClient;

    @Captor
    ArgumentCaptor<SetIdentityNotificationTopicRequest> requestCaptor;

    @Before
    public void setUp() throws Exception {

        client = new SesClientImpl(mockAwsSesClient);

    }

    @Test
    public void testSetBounceNotificationTopic() {

        // call under test
        client.setBounceNotificationTopic("myDomain", "myTopicArn");

        verify(mockAwsSesClient).setIdentityNotificationTopic(requestCaptor.capture());
        assertEquals("myTopicArn", requestCaptor.getValue().snsTopic());
        assertEquals("myDomain", requestCaptor.getValue().identity());
        assertEquals("Bounce", String.valueOf(requestCaptor.getValue().notificationType()));

    }

    @Test
    public void testSetComplaintNotificationTopic() {

        // call under test
        client.setComplaintNotificationTopic("myDomain", "myTopicArn");

        verify(mockAwsSesClient).setIdentityNotificationTopic(requestCaptor.capture());
        assertEquals("myTopicArn", requestCaptor.getValue().snsTopic());
        assertEquals("myDomain",requestCaptor.getValue().identity());
        assertEquals("Complaint", String.valueOf(requestCaptor.getValue().notificationType()));

    }

}