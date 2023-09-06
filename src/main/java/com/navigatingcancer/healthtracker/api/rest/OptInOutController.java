package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.model.OptInOut;
import com.navigatingcancer.healthtracker.api.data.service.OptInOutService;
import java.util.List;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController("/optin")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class OptInOutController implements OptInOutAPI {

  @Autowired private OptInOutService optInOutService;

  @Override
  public List<OptInOut> getOptInOutRecords(
      Long clinicId, Long locationId, Long patientId, String action, String surveyId) {
    OptInOut.validateInputValues(clinicId, action);

    return optInOutService.getOptInOutRecords(clinicId, locationId, patientId, action, surveyId);
  }

  @Override
  public OptInOut optInOut(@Valid OptInOut body) {
    return optInOutService.save(body);
  }

  @Override
  public OptInOut getOptInOutStatus(Long clinicId, Long patientId, String surveyId) {
    OptInOut.validateInputValues(clinicId, null);
    List<OptInOut> l =
        optInOutService.getOptInOutRecords(clinicId, null, patientId, null, surveyId);
    OptInOut resp = null;
    if (l.size() == 1) {
      resp = l.get(0);
    } else if (l.size() > 0) {
      long lastDate = l.get(0).getActionTimestamp().getTime();
      for (OptInOut o : l) {
        if (o.getActionTimestamp().getTime() >= lastDate) {
          resp = o;
          lastDate = o.getActionTimestamp().getTime();
        }
      }
    }
    return resp;
  }
}
