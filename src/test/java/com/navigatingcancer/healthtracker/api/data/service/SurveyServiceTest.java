package com.navigatingcancer.healthtracker.api.data.service;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.repo.surveyInstance.SurveyInstanceRepository;
import com.navigatingcancer.healthtracker.api.rest.exception.RecordNotFoundException;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class SurveyServiceTest {
  @Autowired private SurveyService surveyService;
  @MockBean private SurveyInstanceRepository svInstanceRepository;

  @Test
  public void getSurveyInstance_throwsNoSuchElementExceptionIfSurveyInstanceIdNotFound() {
    String badId = "1234svinstance";
    Mockito.when(svInstanceRepository.findById(Mockito.any())).thenReturn(Optional.empty());
    Assert.assertThrows(
        RecordNotFoundException.class, () -> surveyService.getSurveyInstance(badId));
  }
}
