package com.navigatingcancer.healthtracker.api.rest;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveySubmission;
import com.navigatingcancer.healthtracker.api.data.service.SurveySubmissionService;
import com.navigatingcancer.healthtracker.api.rest.exception.RestErrorHandler;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SurveySubmissionControllerTest {
  @MockBean private SurveySubmissionService surveySubmissionService;
  @InjectMocks private SurveySubmissionController surveySubmissionController;
  @Autowired private ObjectMapper mapper;
  private MockMvc mvc;

  @Before
  public void before() throws Exception {
    MockitoAnnotations.openMocks(this).close();
    this.mvc =
        MockMvcBuilders.standaloneSetup(surveySubmissionController)
            .setControllerAdvice(new RestErrorHandler())
            .build();
  }

  @Test
  public void submitSurvey_happyPath() throws Exception {
    // define submission
    SurveySubmission surveySubmission = new SurveySubmission();
    surveySubmission.setCheckInId("testId");
    Map<String, Object> payload = new HashMap<String, Object>();
    payload.put("testSymptom", "It hurts");
    payload.put("declineACall", false);
    surveySubmission.setSurveyPayload(payload);
    mvc.perform(
            post("/survey-submission")
                .content(mapper.writeValueAsBytes(surveySubmission))
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn();

    verify(surveySubmissionService, times(1)).process(surveySubmission);
  }
}
