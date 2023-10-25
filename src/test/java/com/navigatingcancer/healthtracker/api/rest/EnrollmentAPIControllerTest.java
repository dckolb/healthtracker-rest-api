package com.navigatingcancer.healthtracker.api.rest;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.rest.exception.RestErrorHandler;
import com.navigatingcancer.sqs.SqsHelper;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class EnrollmentAPIControllerTest {

  @Mock EnrollmentService mockEnrollmentService;

  @InjectMocks private EnrollmentAPIController enrollmentApiController;

  @MockBean SqsHelper sqsHelper;

  @MockBean Identity identity;

  private MockMvc mvc;
  private ObjectMapper mapper;
  private ObjectWriter ow;

  @Before
  public void setUp() throws Exception {
    mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    ow = mapper.writer().withDefaultPrettyPrinter();
    MockitoAnnotations.initMocks(this);

    this.mvc =
        MockMvcBuilders.standaloneSetup(enrollmentApiController)
            .setControllerAdvice(new RestErrorHandler())
            .build();
  }

  @Test
  public void getEnrollment() {}

  @Test
  public void getEnrollments() {}

  @Test
  public void getCurrentEnrollments() {}

  @Test
  public void getCheckInDataByEnrollmentIDs_validInput() throws Exception {
    String[] enrollments = {"enrollmentID1"};
    List<String> enrollmentIDs = new ArrayList<>();
    List<Enrollment> enrollmentList = new ArrayList<>();
    enrollmentIDs.add("enrollmentID1");
    String requestJson = ow.writeValueAsString(enrollments);

    ArgumentCaptor<List> queryArgumentCaptor = ArgumentCaptor.forClass(List.class);

    given(enrollmentApiController.getEnrollmentsByIds(enrollmentIDs)).willReturn(enrollmentList);

    mvc.perform(
            post("/enrollments/by-ids")
                .content(requestJson)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
        .andDo(print())
        .andExpect(status().isOk());

    verify(mockEnrollmentService).getEnrollmentsByIds(queryArgumentCaptor.capture());

    Assert.assertEquals(
        "EnrollmentService called with right locationId",
        String.join(",", enrollments),
        queryArgumentCaptor.getValue().iterator().next().toString());
  }

  @Test
  public void saveEnrollment_updatesIfEnrollmentIdPresent() throws Exception {
    Enrollment enrollment = new Enrollment();
    enrollment.setId("enrollmentId");
    enrollment.setVersion(1l);
    enrollment.setPatientId(5l);
    enrollment.setClinicId(4l);
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    enrollment.setSchedules(new ArrayList<>());

    String requestJson = ow.writeValueAsString(enrollment);
    ArgumentCaptor<Enrollment> queryArgumentCaptor = ArgumentCaptor.forClass(Enrollment.class);
    mvc.perform(
            post("/enrollments").content(requestJson).contentType(MediaType.APPLICATION_JSON_UTF8))
        .andDo(print())
        .andExpect(status().isOk());

    verify(mockEnrollmentService, times(1)).updateEnrollment(queryArgumentCaptor.capture());
    Assert.assertEquals(enrollment.getId(), queryArgumentCaptor.getValue().getId());
  }

  @Test
  public void saveEnrollment_createsIfEnrollmentIdAbsent() throws Exception {
    Enrollment enrollment = new Enrollment();
    enrollment.setPatientId(5l);
    enrollment.setClinicId(4l);
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    enrollment.setSchedules(new ArrayList<>());

    String requestJson = ow.writeValueAsString(enrollment);

    mvc.perform(
            post("/enrollments").content(requestJson).contentType(MediaType.APPLICATION_JSON_UTF8))
        .andDo(print())
        .andExpect(status().isOk());

    verify(mockEnrollmentService, times(1)).createEnrollment(any());
  }

  @Test
  public void saveEnrollment_returns400IfIdSetWithoutVersion() throws Exception {
    Enrollment enrollment = new Enrollment();
    enrollment.setId("enrollmentId");
    enrollment.setPatientId(5l);
    enrollment.setClinicId(4l);
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    enrollment.setSchedules(new ArrayList<>());

    String requestJson = ow.writeValueAsString(enrollment);

    mvc.perform(
            post("/enrollments").content(requestJson).contentType(MediaType.APPLICATION_JSON_UTF8))
        .andDo(print())
        .andExpect(status().is4xxClientError());
  }

  @Test
  public void changeStatus() {}

  @Test
  public void updateEnrollment() {}
}
