package com.navigatingcancer.healthtracker.api.data.service;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.service.impl.PatientRequestServiceImpl;
import com.navigatingcancer.healthtracker.api.processor.model.TriageRequestPayload;
import com.navigatingcancer.healthtracker.api.rest.representation.PatientRequest;
import com.navigatingcancer.json.utils.JsonUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class PatientRequestServiceTest {

    @Autowired
    private PatientRequestService patientRequestService;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    public void givenCallRequest_shouldCallRabbit(){
        String message = UUID.randomUUID().toString();
        PatientRequest patientRequest = new PatientRequest(5l,1l,message);

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        this.patientRequestService.requestCall(patientRequest);

        verify(rabbitTemplate, times(1)).convertAndSend(any(String.class), stringArgumentCaptor.capture());

        String captured = stringArgumentCaptor.getValue();
        TriageRequestPayload payload = JsonUtils.fromJson(captured, TriageRequestPayload.class);

        Assert.assertTrue(payload.getReasons().get(0).getDetails().get(0).contains(message));
    }
    @Test
    public void givenRefillRequest_shouldCallRabbit(){
        String message = UUID.randomUUID().toString();
        PatientRequest patientRequest = new PatientRequest(5l,1l,message);

        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        this.patientRequestService.requestRefill(patientRequest);

        verify(rabbitTemplate, times(1)).convertAndSend(any(String.class), stringArgumentCaptor.capture());
        String captured = stringArgumentCaptor.getValue();

        TriageRequestPayload payload = JsonUtils.fromJson(captured, TriageRequestPayload.class);

        Assert.assertEquals(message, payload.getReasons().get(0).getDetails().get(0));
    }
}
