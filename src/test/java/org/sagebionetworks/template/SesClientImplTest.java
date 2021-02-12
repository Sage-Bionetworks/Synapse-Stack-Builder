package org.sagebionetworks.template;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SetIdentityNotificationTopicRequest;
import com.amazonaws.services.simpleemail.model.SetIdentityNotificationTopicResult;
import net.bytebuddy.asm.Advice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SesClientImplTest {

    SesClientImpl client;

    @Mock
    AmazonSimpleEmailService mockAwsSesClient;

    @Captor
    ArgumentCaptor<SetIdentityNotificationTopicRequest> setIdentityNotificationTopicRequestArgumentCaptor;

    SetIdentityNotificationTopicResult expectedResult;

    @Before
    public void setUp() throws Exception {

        client = new SesClientImpl(mockAwsSesClient);

        expectedResult = new SetIdentityNotificationTopicResult();

        when(mockAwsSesClient.setIdentityNotificationTopic(any(SetIdentityNotificationTopicRequest.class))).thenReturn(expectedResult);
    }

    @Test
    public void testSetBounceNotificationTopic() {

        // call under test
        client.setBounceNotificationTopic("myDomain", "myTopicArn");

        verify(mockAwsSesClient).setIdentityNotificationTopic(setIdentityNotificationTopicRequestArgumentCaptor.capture());
        assertEquals("myTopicArn", setIdentityNotificationTopicRequestArgumentCaptor.getValue().getSnsTopic());
        assertEquals("myDomain", setIdentityNotificationTopicRequestArgumentCaptor.getValue().getIdentity());
        assertEquals("Bounce", setIdentityNotificationTopicRequestArgumentCaptor.getValue().getNotificationType());

    }

    @Test
    public void testSetComplaintNotificationTopic() {

        // call under test
        client.setComplaintNotificationTopic("myDomain", "myTopicArn");

        verify(mockAwsSesClient).setIdentityNotificationTopic(setIdentityNotificationTopicRequestArgumentCaptor.capture());
        assertEquals("myTopicArn", setIdentityNotificationTopicRequestArgumentCaptor.getValue().getSnsTopic());
        assertEquals("myDomain", setIdentityNotificationTopicRequestArgumentCaptor.getValue().getIdentity());
        assertEquals("Complaint", setIdentityNotificationTopicRequestArgumentCaptor.getValue().getNotificationType());

    }

}