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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SesClientImplTest {

    SesClientImpl client;

    @Mock
    software.amazon.awssdk.services.ses.SesClient mockAwsSesClient;

    @Captor
    ArgumentCaptor<SetIdentityNotificationTopicRequest> setIdentityNotificationTopicRequestArgumentCaptor;

    @Before
    public void setUp() throws Exception {

        client = new SesClientImpl(mockAwsSesClient);

        when(mockAwsSesClient.setIdentityNotificationTopic(any(SetIdentityNotificationTopicRequest.class))).thenReturn(null);
    }

    @Test
    public void testSetBounceNotificationTopic() {

        // call under test
        client.setBounceNotificationTopic("myDomain", "myTopicArn");

        verify(mockAwsSesClient).setIdentityNotificationTopic(setIdentityNotificationTopicRequestArgumentCaptor.capture());
        assertEquals("myTopicArn", String.valueOf(setIdentityNotificationTopicRequestArgumentCaptor.getValue().snsTopic()));
        assertEquals("myDomain", String.valueOf(setIdentityNotificationTopicRequestArgumentCaptor.getValue().identity()));
        assertEquals("Bounce", String.valueOf(setIdentityNotificationTopicRequestArgumentCaptor.getValue().notificationType()));

    }

    @Test
    public void testSetComplaintNotificationTopic() {

        // call under test
        client.setComplaintNotificationTopic("myDomain", "myTopicArn");

        verify(mockAwsSesClient).setIdentityNotificationTopic(setIdentityNotificationTopicRequestArgumentCaptor.capture());
        assertEquals("myTopicArn", String.valueOf(setIdentityNotificationTopicRequestArgumentCaptor.getValue().snsTopic()));
        assertEquals("myDomain", String.valueOf(setIdentityNotificationTopicRequestArgumentCaptor.getValue().identity()));
        assertEquals("Complaint", String.valueOf(setIdentityNotificationTopicRequestArgumentCaptor.getValue().notificationType()));

    }

}