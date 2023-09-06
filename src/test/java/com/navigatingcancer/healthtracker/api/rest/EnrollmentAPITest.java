package com.navigatingcancer.healthtracker.api.rest;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepositoryTest;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import com.navigatingcancer.healthtracker.api.rest.exception.DuplicateEnrollmentException;
import com.navigatingcancer.healthtracker.api.rest.exception.RestErrorHandler;
import java.net.URI;
import java.security.InvalidParameterException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class EnrollmentAPITest {

  @Mock private EnrollmentService service;

  @Autowired private ObjectMapper mapper;

  @InjectMocks private EnrollmentAPIController enrollmentController;

  private MockMvc mvc;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    this.mvc =
        MockMvcBuilders.standaloneSetup(enrollmentController)
            .setControllerAdvice(new RestErrorHandler())
            .build();
  }

  @Test
  public void givenStartAndEndDateAsString_shouldParsetoLocalDate() throws Exception {

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

    params.add("startDate", "2018-05-09");
    params.add("endDate", "2018-05-12");

    Enrollment e = EnrollmentRepositoryTest.createEnrollment(2, 2, 2);

    given(service.getEnrollments(any(EnrollmentQuery.class))).willReturn(Arrays.asList(e));

    mvc.perform(get("/enrollments").params(params).contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  public void givenId_shouldReturnEnrollment() throws Exception {
    String id = UUID.randomUUID().toString();

    Enrollment e = EnrollmentRepositoryTest.createEnrollment(2, 2, 2);

    given(service.getEnrollment(any(String.class))).willReturn(e);

    mvc.perform(get("/enrollments/" + id).contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  public void givenNoFindParameters_shouldReturnBadRequest() throws Exception {

    mvc.perform(get("/enrollments").contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  public void givenEnrollment_shouldSaveEnrollment() throws Exception {

    Enrollment e = EnrollmentRepositoryTest.createEnrollment(2, 2, 2);

    Enrollment e2 = EnrollmentRepositoryTest.createEnrollment(22, 22, 22);
    given(service.createEnrollment(any(Enrollment.class))).willReturn(e2);

    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    String requestJson = ow.writeValueAsString(e);

    mvc.perform(
            post("/enrollments").content(requestJson).contentType(MediaType.APPLICATION_JSON_UTF8))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  public void givenInvalidEnrollment_shouldReturnBadRequest() throws Exception {

    // Empty enrollment
    Enrollment e = new Enrollment();
    e.setReminderTimeZone("America/Hawaii"); // invalid TZ

    given(service.createEnrollment(any(Enrollment.class))).willThrow(new NullPointerException());

    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    String requestJson = ow.writeValueAsString(e);

    MvcResult result =
        mvc.perform(
                post("/enrollments")
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andReturn();
    String responseString = result.getResponse().getContentAsString();

    Assert.assertTrue(
        "Error message for reminderTimeZone",
        responseString.contains("reminderTimeZone is not a valid timezone"));
    Assert.assertTrue(
        "Error message for PatientId", responseString.contains("patientId must not be null"));
    Assert.assertTrue(
        "Error message for schedules", responseString.contains("schedules must not be empty"));
    Assert.assertTrue(
        "Error message for daysInCycle", responseString.contains("daysInCycle must not be null"));
    Assert.assertTrue(
        "Error message for reminderTime", responseString.contains("reminderTime must not be null"));
    Assert.assertTrue(
        "Error message for clinicId", responseString.contains("clinicId must not be null"));
    Assert.assertTrue(
        "Error message for alerts", responseString.contains("alerts must not be null"));
  }

  @Test
  public void givenSchedulingError_shouldReturnInternalServerError() throws Exception {

    Enrollment e = EnrollmentRepositoryTest.createEnrollment(2, 2, 2);

    given(service.createEnrollment(any(Enrollment.class))).willThrow(new NullPointerException());

    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    String requestJson = ow.writeValueAsString(e);

    mvc.perform(
            post("/enrollments").content(requestJson).contentType(MediaType.APPLICATION_JSON_UTF8))
        .andDo(print())
        .andExpect(status().isInternalServerError());
  }

  @Test
  public void givenDuplicateEnrollment_shouldReturnConflict() throws Exception {
    Enrollment e = EnrollmentRepositoryTest.createEnrollment(2, 2, 2);

    given(service.createEnrollment(any(Enrollment.class)))
        .willThrow(DuplicateEnrollmentException.class);
    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    String requestJson = ow.writeValueAsString(e);

    mvc.perform(
            post("/enrollments").content(requestJson).contentType(MediaType.APPLICATION_JSON_UTF8))
        .andDo(print())
        .andExpect(status().isConflict());
  }

  @Test
  public void givenActiveEnrollment_shouldReturnSuccess() throws Exception {
    String id = UUID.randomUUID().toString();

    Enrollment e = EnrollmentRepositoryTest.createEnrollment(2, 2, 2);

    given(service.getEnrollment(any(String.class))).willReturn(e);

    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    String requestJson = ow.writeValueAsString(e);

    mvc.perform(
            get("/enrollments/" + id).content(requestJson).contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  public void givenActiveEnrollment_shouldReturnCurrentCycleInfo() throws Exception {
    String id = UUID.randomUUID().toString();

    Enrollment e = EnrollmentRepositoryTest.createEnrollment(2, 2, 2766);
    e.setDaysInCycle(21);
    e.setTxStartDate(LocalDate.now().minusDays(22));

    given(service.getEnrollment(any(String.class))).willReturn(e);

    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    String requestJson = ow.writeValueAsString(e);

    mvc.perform(
            get("/enrollments/" + id).content(requestJson).contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentCycleNumber", is(2)));
  }

  @Test
  public void updateEnrollment_givenActiveEnrollment_shouldReturnSuccess() throws Exception {
    String id = UUID.randomUUID().toString();

    Enrollment e = EnrollmentRepositoryTest.createEnrollment(2, 2, 2);
    e.setId(id);
    e.setStatus(EnrollmentStatus.ACTIVE);

    given(service.getEnrollment(any(String.class))).willReturn(e);

    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    String requestJson = ow.writeValueAsString(e);
    URI u = new URI("/enrollments/" + id);

    mvc.perform(
            MockMvcRequestBuilders.put(u)
                .content(requestJson)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(""))
        .andExpect(status().isOk());
  }

  @Test
  public void
      updateEnrollment_givenActiveEnrollment_shouldReturnInternalServerError_nonMatchingIDs()
          throws Exception {
    String id = UUID.randomUUID().toString();

    Enrollment e = EnrollmentRepositoryTest.createEnrollment(2, 2, 2);
    e.setStatus(EnrollmentStatus.ACTIVE);

    given(service.getEnrollment(any(String.class))).willReturn(e);

    Enrollment newe = e;
    CheckInSchedule schedule = new CheckInSchedule();
    schedule.setMedication("gumdrops");
    List<CheckInSchedule> newschedules = new ArrayList<CheckInSchedule>();
    newschedules.add(schedule);
    newe.setSchedules(newschedules);

    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    String requestJson = ow.writeValueAsString(newe);
    URI u = new URI("/enrollments/" + id);

    mvc.perform(
            MockMvcRequestBuilders.put(u)
                .content(requestJson)
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isInternalServerError());
  }

  @Test
  public void
      updateEnrollment_givenActiveEnrollment_shouldReturnInternalServerError_differingCheckInScheduleCount()
          throws Exception {
    // Arrange
    String id = UUID.randomUUID().toString();
    Enrollment e = EnrollmentRepositoryTest.createEnrollment(2, 2, 2);
    e.setId(id);
    e.setStatus(EnrollmentStatus.ACTIVE);
    e.setUrl("testing");

    Enrollment newe = e;

    given(service.updateEnrollment(newe)).willThrow(new InvalidParameterException());

    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    String requestJson = ow.writeValueAsString(newe);
    URI u = new URI("/enrollments/" + newe.getId());

    // Act & Assert
    mvc.perform(
            MockMvcRequestBuilders.put(u)
                .content(requestJson)
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isInternalServerError());
  }

  @Test
  public void
      updateEnrollment_givenActiveEnrollment_shouldReturnInternalServerError_noMatchingCheckInSchedule()
          throws Exception {
    String id = UUID.randomUUID().toString();

    Enrollment e = EnrollmentRepositoryTest.createEnrollment(2, 2, 2);
    e.setId(id);
    e.setStatus(EnrollmentStatus.ACTIVE);
    e.setUrl("testing");

    Enrollment newe = e;

    given(service.updateEnrollment(newe)).willThrow(new InvalidParameterException());

    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    String requestJson = ow.writeValueAsString(newe);
    URI u = new URI("/enrollments/" + newe.getId());

    mvc.perform(
            MockMvcRequestBuilders.put(u)
                .content(requestJson)
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isInternalServerError());
  }
}
