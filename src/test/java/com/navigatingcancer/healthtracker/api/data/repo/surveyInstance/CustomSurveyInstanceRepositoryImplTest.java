package com.navigatingcancer.healthtracker.api.data.repo.surveyInstance;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyInstance;
import java.util.SortedMap;
import java.util.TreeMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CustomSurveyInstanceRepositoryImplTest {
  @Autowired private SurveyInstanceRepository surveyInstanceRepository;
  SortedMap<String, Object> params =
      new TreeMap<>() {
        {
          put("foo", "bar");
        }
      };
  SurveyInstance testInstance = new SurveyInstance(1L, 2L, "3", params);

  @Before
  public void setup() {
    surveyInstanceRepository.deleteAll();
    surveyInstanceRepository.save(testInstance);
  }

  @Test
  public void findByHash_findsWithHash() {
    SurveyInstance returnInstance = surveyInstanceRepository.findByHash(testInstance.getHash());

    Assert.assertEquals(testInstance, returnInstance);
  }

  @Test
  public void insertIgnore_doesNotInsertOnKeyCollision() {
    SurveyInstance returnInstance = surveyInstanceRepository.insertIgnore(testInstance);

    Assert.assertEquals(1L, surveyInstanceRepository.count());
    Assert.assertEquals(testInstance, returnInstance);
  }

  @Test
  public void insertIgnore_doesInsertIfKeyUnique() {
    SurveyInstance testInstance2 = new SurveyInstance(4L, 5L, "6", params);
    SurveyInstance returnInstance = surveyInstanceRepository.insertIgnore(testInstance2);

    Assert.assertEquals(2L, surveyInstanceRepository.count());
    Assert.assertEquals(testInstance2, returnInstance);
  }
}
