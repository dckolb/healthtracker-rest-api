package com.navigatingcancer.healthtracker.api.data.service;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.client.DocusignServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.SigningRequest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class DocusignServiceTest {

    @MockBean
    private DocusignServiceClient client;

    @Autowired
    private DocusignService docusignService;

    @Test
    public void givenSigningRequestResponse_shouldReturnId(){

        SigningRequest response = new SigningRequest();
        response.setId(UUID.randomUUID().toString());
        when(client.createSigningRequest(any(SigningRequest.class))).thenReturn(response);

        SigningRequest signingRequest = new SigningRequest();

        String id = this.docusignService.sendSigningRequest(signingRequest);

        Assert.assertEquals("id's should match", id, response.getId());
    }

    @Test
    public void givenId_shouldReturnStatus(){
        SigningRequest response = new SigningRequest();
        response.setStatus("COMPLETED");
        when(client.getSigningRequest(any(String.class))).thenReturn(response);
        String id = UUID.randomUUID().toString();
        String result = this.docusignService.getSigningRequestStatus(id);
        Assert.assertEquals("status should be COMPLETED", response.getStatus(), result);
    }

    @Test
    public void givenId_shouldResend() {
        doNothing().when(client).resendRequest(anyString());
        this.docusignService.resendSigningRequest(UUID.randomUUID().toString());
        verify(client, times(1)).resendRequest(anyString());
    }

}
