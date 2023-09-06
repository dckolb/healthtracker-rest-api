package com.navigatingcancer.healthtracker.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.model.ProReview;
import com.navigatingcancer.healthtracker.api.data.model.ProReviewNote;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.service.ProReviewService;
import com.navigatingcancer.healthtracker.api.rest.exception.RestErrorHandler;
import com.navigatingcancer.healthtracker.api.rest.representation.ProReviewResponse;
import com.navigatingcancer.healthtracker.api.rest.representation.ProReviewUpdateRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// TODO add support for testing validation
@RunWith(SpringJUnit4ClassRunner.class)
public class ProReviewAPIControllerTest {
  @Mock ProReviewService proReviewService;

  @InjectMocks private ProReviewAPIController proReviewAPIController;

  @MockBean Identity identity;

  private MockMvc mvc;
  private ObjectMapper mapper;

  // private String invalidId = "1";
  private String validId = "111111111111111111111111";

  @Before
  public void setUp() throws Exception {
    mapper = new ObjectMapper();
    MockitoAnnotations.initMocks(this);

    this.mvc =
        MockMvcBuilders.standaloneSetup(proReviewAPIController)
            .setControllerAdvice(new RestErrorHandler())
            .build();
  }

  @Test
  public void getProReviewHandler_returns200WithValidId() throws Exception {
    List<String> checkInIds = Arrays.asList("check in id");
    ProReview proReview =
        new ProReview(
            "1",
            1L,
            "enrollmentId",
            checkInIds,
            new SurveyPayload(),
            Collections.emptyList(),
            Collections.emptyList(),
            null,
            null,
            null);
    List<ProReviewNote> proReviewNotes =
        Arrays.asList(new ProReviewNote(validId, "test note", "dr test", new Date(1L)));

    ProReviewResponse response = new ProReviewResponse(proReview, proReviewNotes);

    given(proReviewService.getProReview(any(String.class))).willReturn(response);

    MvcResult result =
        mvc.perform(get(String.format("/pro-review/%s", validId)))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

    String returnedJson = result.getResponse().getContentAsString();
    Assert.assertTrue(returnedJson.contains("\"id\":\"1\""));
  }

  // @Test
  // public void getProReviewHandler_returns400WithInvalidId() throws Exception {
  //   mvc.perform(get(String.format("/pro-review/%s", invalidId)))
  //       .andDo(print())
  //       .andExpect(status().isBadRequest());
  // }

  @Test
  public void createProReviewNoteHandler_returns200WithValidId() throws Exception {
    ProReviewUpdateRequest request =
        new ProReviewUpdateRequest(
            "testId",
            new ArrayList<String>(),
            "testNote",
            HealthTrackerStatusCategory.ACTION_NEEDED,
            true,
            null);
    given(identity.getClinicianName()).willReturn("dr test");

    mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    String requestJson = ow.writeValueAsString(request);
    mvc.perform(
            patch(String.format("/pro-review/%s", validId))
                .content(requestJson)
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk());
  }
}
