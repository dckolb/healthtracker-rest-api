package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.service.PatientRequestService;
import com.navigatingcancer.healthtracker.api.rest.representation.PatientRequest;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController("/request")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@Slf4j
public class RequestAPIController implements RequestAPI {

  private PatientRequestService patientRequestService;

  @Autowired
  public RequestAPIController(PatientRequestService patientRequestService) {
    this.patientRequestService = patientRequestService;
  }

  @Override
  public void requestCallHandler(@Valid PatientRequest callRequest) {
    this.patientRequestService.requestCall(callRequest);
  }

  @Override
  public void requestRefillHandler(@Valid PatientRequest refillRequest) {
    this.patientRequestService.requestRefill(refillRequest);
  }
}
