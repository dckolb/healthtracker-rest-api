package com.navigatingcancer.healthtracker.api.data.model.survey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navigatingcancer.healthtracker.api.processor.DefaultDroolsServiceTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class SurveyPayloadTest {

  @Test
  public void testSerializationDeserialization() throws Exception {

    // test serialization
    SurveyPayload payload = new SurveyPayload();

    List<SurveyItemPayload> symptomsList = new ArrayList<>();
    symptomsList.add(
        DefaultDroolsServiceTest.createSurvey(
            "nauseaSeverity", "2", "painSeverity", "4", "constipationSeverity", "1"));
    List<SurveyItemPayload> oralList = new ArrayList<>();
    oralList.add(DefaultDroolsServiceTest.createSurvey("medicationTaken", "no"));

    payload.setSymptoms(symptomsList);
    payload.setOral(oralList);

    // test serialization
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(payload);
    String expected =
        "{\"surveyPayload\":{\"enrollmentId\":null,\"medication\":[{\"id\":null,\"payload\":{\"medicationTaken\":\"no\"},\"declineACall\":false,\"declineACallComment\":null}],\"symptoms\":[{\"id\":null,\"payload\":{\"nauseaSeverity\":\"2\",\"constipationSeverity\":\"1\",\"painSeverity\":\"4\"},\"declineACall\":false,\"declineACallComment\":null}]}}";

    Assert.assertEquals(expected, json);

    // test deserialization
    SurveyPayload deserializedPayload = mapper.readValue(json, SurveyPayload.class);
    SurveyPayloadContent deserializedContent = deserializedPayload.getContent();
    Assert.assertNotNull(deserializedContent);

    Assert.assertEquals(payload.getOral(), deserializedContent.getOral());
    Assert.assertEquals(payload.getSymptoms(), deserializedContent.getSymptoms());
  }
}
