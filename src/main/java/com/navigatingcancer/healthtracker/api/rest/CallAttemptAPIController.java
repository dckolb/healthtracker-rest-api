package com.navigatingcancer.healthtracker.api.rest;

import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.CallAttempt;
import com.navigatingcancer.healthtracker.api.data.service.CallAttemptService;
import com.navigatingcancer.healthtracker.api.events.HealthTrackerEventsPublisher;
import java.util.List;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController("/callattempts")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class CallAttemptAPIController implements CallAttemptAPI {

  private static final Logger logger = LoggerFactory.getLogger(CallAttemptAPIController.class);

  private CallAttemptService callAttemptService;

  @Autowired Identity identity;

  @Autowired HealthTrackerEventsPublisher eventsPublisher;

  @Autowired
  public CallAttemptAPIController(CallAttemptService callAttemptService) {
    this.callAttemptService = callAttemptService;
  }

  @Override
  public CallAttempt saveCallAttempt(@Valid CallAttempt callAttempt) {
    logger.debug("CallAttemptAPIController::saveCallAttempt {}", callAttempt);

    callAttempt.setId(null);

    CallAttempt res = callAttemptService.saveCallAttempt(callAttempt);
    eventsPublisher.publishCallAttempt(res, identity);

    return res;
  }

  @Override
  public List<CallAttempt> getCallAttempts(List<String> checkInIds) {
    logger.debug("CallAttemptAPIController::getCallAttempts {}", checkInIds);

    return callAttemptService.getCallAttempts(checkInIds);
  }
}
