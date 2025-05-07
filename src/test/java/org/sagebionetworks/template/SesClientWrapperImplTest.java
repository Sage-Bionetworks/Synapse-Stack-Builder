package org.sagebionetworks.template;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.NotificationType;
import software.amazon.awssdk.services.ses.model.SetIdentityNotificationTopicRequest;
import software.amazon.awssdk.services.ses.model.SetIdentityNotificationTopicResponse;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SesClientWrapperImplTest {

    SesClientWrapperImpl client;

    @Mock
    SesClient mockAwsSesClient;

    @Captor
    ArgumentCaptor<SetIdentityNotificationTopicRequest> setIdentityNotificationTopicRequestArgumentCaptor;

    SetIdentityNotificationTopicResponse expectedResponse;

    @Before
    public void setUp() throws Exception {

        client = new SesClientWrapperImpl(mockAwsSesClient);

        expectedResponse = SetIdentityNotificationTopicResponse.builder().build();
        when(mockAwsSesClient.setIdentityNotificationTopic(any(SetIdentityNotificationTopicRequest.class))).thenReturn(expectedResponse);
    }

    @Test
    public void testSetBounceNotificationTopic() {

        // call under test
        client.setBounceNotificationTopic("myDomain", "myTopicArn");

        verify(mockAwsSesClient).setIdentityNotificationTopic(setIdentityNotificationTopicRequestArgumentCaptor.capture());
        assertEquals("myTopicArn", setIdentityNotificationTopicRequestArgumentCaptor.getValue().snsTopic());
        assertEquals("myDomain", setIdentityNotificationTopicRequestArgumentCaptor.getValue().identity());
        assertEquals(NotificationType.BOUNCE, setIdentityNotificationTopicRequestArgumentCaptor.getValue().notificationType());

    }

    @Test
    public void testSetComplaintNotificationTopic() {

        // call under test
        client.setComplaintNotificationTopic("myDomain", "myTopicArn");

        verify(mockAwsSesClient).setIdentityNotificationTopic(setIdentityNotificationTopicRequestArgumentCaptor.capture());
        assertEquals("myTopicArn", setIdentityNotificationTopicRequestArgumentCaptor.getValue().snsTopic());
        assertEquals("myDomain", setIdentityNotificationTopicRequestArgumentCaptor.getValue().identity());
        assertEquals(NotificationType.COMPLAINT, setIdentityNotificationTopicRequestArgumentCaptor.getValue().notificationType());

    }

}