package com.navigatingcancer.healthtracker.api.rest;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
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

  @Before
  public void setUp() throws Exception {
    mapper = new ObjectMapper();
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

    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
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
  public void saveEnrollment() {}

  @Test
  public void changeStatus() {}

  @Test
  public void updateEnrollment() {}
}
