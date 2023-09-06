package com.navigatingcancer.healthtracker.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.CheckInData;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.service.StatusService;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.healthtracker.api.rest.exception.RestErrorHandler;
import com.navigatingcancer.healthtracker.api.rest.representation.HealthTrackerStatusResponse;
import com.navigatingcancer.healthtracker.api.rest.representation.StatusRequest;
import com.navigatingcancer.patientinfo.domain.PatientInfo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class StatusControllerTest {

  @Mock private StatusService service;

  @Mock HealthTrackerStatusService healthTrackerStatusService;

  @Autowired private ObjectMapper mapper;

  @InjectMocks private HealthTrackerStatusController statusController;

  private MockMvc mvc;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    this.mvc =
        MockMvcBuilders.standaloneSetup(statusController)
            .setControllerAdvice(new RestErrorHandler())
            .build();
  }

  @Test
  public void givenIds_shouldCallStatusService_andReturnList() throws Exception {

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

    params.add("clinicId", "2345");
    params.add("ids", "9874");

    HealthTrackerStatus htStatus = new HealthTrackerStatus();
    htStatus.setClinicId(1234l);
    PatientInfo pi = new PatientInfo();
    pi.setId(1234l);
    htStatus.setPatientInfo(pi);
    List<HealthTrackerStatus> svcResults = new ArrayList<>(Arrays.asList(htStatus));

    given(service.getByIds(any(Long.class), any(List.class))).willReturn(svcResults);

    MvcResult result =
        mvc.perform(get("/status").params(params).contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

    List<HealthTrackerStatus> mapped =
        this.mapper.readValue(
            result.getResponse().getContentAsByteArray(),
            new TypeReference<List<HealthTrackerStatus>>() {});

    Assert.assertEquals(svcResults, mapped);
  }

  @Test
  public void givenStatusRequest_shouldCallStatusService_andReturnList() throws Exception {

    StatusRequest statusRequest = new StatusRequest();
    statusRequest.setClinicId(2345l);
    statusRequest.setIds(Arrays.asList("9874"));

    HealthTrackerStatus htStatus = new HealthTrackerStatus();
    htStatus.setClinicId(1234l);
    PatientInfo pi = new PatientInfo();
    pi.setId(1234l);
    htStatus.setPatientInfo(pi);
    List<HealthTrackerStatus> svcResults = new ArrayList<>(Arrays.asList(htStatus));

    given(service.getByIds(any(Long.class), any(List.class))).willReturn(svcResults);

    MvcResult result =
        mvc.perform(
                post("/status")
                    .content(mapper.writeValueAsBytes(statusRequest))
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

    List<HealthTrackerStatus> mapped =
        this.mapper.readValue(
            result.getResponse().getContentAsByteArray(),
            new TypeReference<List<HealthTrackerStatus>>() {});

    Assert.assertEquals(svcResults, mapped);
  }

  @Test
  public void givenStatusDueRequest_shouldCallStatusService_andReturnList() throws Exception {

    StatusRequest statusRequest = new StatusRequest();
    statusRequest.setClinicId(2345l);
    statusRequest.setIds(Arrays.asList("9874"));

    HealthTrackerStatus htStatus = new HealthTrackerStatus();
    htStatus.setClinicId(1234l);
    PatientInfo pi = new PatientInfo();
    pi.setId(1234l);
    htStatus.setPatientInfo(pi);

    Enrollment e = new Enrollment();
    e.setId("3333");
    e.setTxStartDate(LocalDate.now());
    e.setCycles(1);
    e.setDaysInCycle(21);

    CheckInData checkin = new CheckInData();

    HealthTrackerStatusResponse sr = new HealthTrackerStatusResponse(htStatus, e, checkin);

    List<HealthTrackerStatusResponse> svcResults = new ArrayList<>();
    svcResults.add(sr);

    given(service.getManualCollectDueByIds(any(Long.class), any(List.class)))
        .willReturn(svcResults);

    MvcResult result =
        mvc.perform(
                post("/status/due")
                    .content(mapper.writeValueAsBytes(statusRequest))
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

    verify(service).getManualCollectDueByIds(any(Long.class), any(List.class));
  }

  @Test
  public void givenStatusChange_shouldCallStatusService_andReturnStatus() throws Exception {
    HealthTrackerStatusCategory statusRequest = HealthTrackerStatusCategory.WATCH_CAREFULLY;

    HealthTrackerStatus htStatus = new HealthTrackerStatus();

    given(
            healthTrackerStatusService.setCategory(
                any(String.class), any(HealthTrackerStatusCategory.class), nullable(List.class)))
        .willReturn(htStatus);

    MvcResult result =
        mvc.perform(
                post("/status/123/category")
                    .content(mapper.writeValueAsBytes(statusRequest))
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

    HealthTrackerStatus mapped =
        this.mapper.readValue(
            result.getResponse().getContentAsByteArray(),
            new TypeReference<HealthTrackerStatus>() {});

    Assert.assertEquals(htStatus, mapped);
  }
}
