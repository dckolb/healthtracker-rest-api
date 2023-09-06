package com.navigatingcancer.healthtracker.api.rest;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.CheckInData;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.service.CheckInService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import com.navigatingcancer.healthtracker.api.rest.exception.RestErrorHandler;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(SpringRunner.class)
@Import(TestConfig.class)
public class CheckInAPIControllerTest {

  @Mock CheckInService checkInService;

  @Mock SchedulingServiceImpl schedulingService;

  @InjectMocks private CheckInAPIController checkInAPIController;

  @MockBean Identity identity;

  private MockMvc mvc;
  private ObjectMapper mapper;

  @Before
  public void setUp() {
    mapper = new ObjectMapper();
    MockitoAnnotations.initMocks(this);

    this.mvc =
        MockMvcBuilders.standaloneSetup(checkInAPIController)
            .setControllerAdvice(new RestErrorHandler())
            .build();
  }

  @Test
  public void getCheckInDataByEnrollmentIDs_validInput() throws Exception {
    List<Long> clinicIds = new ArrayList<>();
    clinicIds.add(123L);
    EnrollmentQuery query = new EnrollmentQuery();
    query.setClinicId(clinicIds);

    CheckInData checkInData = new CheckInData();

    given(checkInService.getCheckInData(query)).willReturn(checkInData);

    mvc.perform(get("/checkins/enrollment-ids").contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  public void getCheckDataByEnrollmentId_validInputs() throws Exception {
    String enrollmentId = "1234";

    CheckInData checkInData = new CheckInData();

    given(checkInService.getCheckInData(enrollmentId)).willReturn(checkInData);

    mvc.perform(get("/checkins/" + enrollmentId).contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  public void getCheckDataByLocationId_validInputs() throws Exception {
    EnrollmentQuery query = new EnrollmentQuery();
    CheckInData checkInData = new CheckInData();
    String locationId = "1234";

    ArgumentCaptor<EnrollmentQuery> queryArgumentCaptor =
        ArgumentCaptor.forClass(EnrollmentQuery.class);

    given(checkInService.getCheckInData(query)).willReturn(checkInData);

    mvc.perform(
            get("/checkins/")
                .param("locationId", locationId)
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk());

    verify(checkInService).getCheckInData(queryArgumentCaptor.capture());

    Assert.assertEquals(
        "CheckInService called with right locationId",
        locationId,
        queryArgumentCaptor.getValue().getLocationId().iterator().next().toString());
  }

  @Test
  public void checkIn_validInputs() throws Exception {
    ArgumentCaptor<SurveyPayload> queryArgumentCaptor =
        ArgumentCaptor.forClass(SurveyPayload.class);

    SurveyPayload surveyPayload = new SurveyPayload();

    String requestBody = mapper.writeValueAsString(surveyPayload);

    mvc.perform(
            post("/checkins/").contentType(MediaType.APPLICATION_JSON_UTF8).content(requestBody))
        .andDo(print())
        .andExpect(status().isOk());

    verify(checkInService).checkIn(queryArgumentCaptor.capture());
    Assert.assertEquals(
        "checkin service is called with survey payload",
        surveyPayload,
        queryArgumentCaptor.getValue());
  }

  @Test
  public void remindMeLater_validInputs() throws Exception {
    String enrollmentId = "1234";
    Integer minutes = 60;

    ArgumentCaptor<String> enrollmentIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> minutesCaptor = ArgumentCaptor.forClass(Integer.class);

    mvc.perform(
            post("/checkins/remindMeLater/")
                .contentType(MediaType.APPLICATION_JSON)
                .param("enrollmentId", enrollmentId)
                .param("minutes", Integer.toString(minutes)))
        .andDo(print())
        .andExpect(status().isOk());

    verify(schedulingService).remindMeLater(enrollmentIdCaptor.capture(), minutesCaptor.capture());

    Assert.assertEquals(
        "scheduling service called with correct enrollment id",
        enrollmentId,
        enrollmentIdCaptor.getValue());

    Assert.assertEquals(
        "scheduling service called with correct # of minutes", minutes, minutesCaptor.getValue());
  }

  @Test
  public void remindMeNow() throws Exception {
    String enrollmentId = "1234";

    ArgumentCaptor<String> enrollmentIdCaptor = ArgumentCaptor.forClass(String.class);

    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();

    mvc.perform(
            post("/checkins/remindMeNow/")
                .contentType(MediaType.APPLICATION_JSON)
                .param("enrollmentId", enrollmentId))
        .andDo(print())
        .andExpect(status().isOk());

    verify(schedulingService).remindMeNow(enrollmentIdCaptor.capture());

    Assert.assertEquals(
        "scheduling service called with correct enrollment id",
        enrollmentId,
        enrollmentIdCaptor.getValue());
  }
}
