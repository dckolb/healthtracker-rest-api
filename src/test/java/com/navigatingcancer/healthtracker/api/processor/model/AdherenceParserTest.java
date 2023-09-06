package com.navigatingcancer.healthtracker.api.processor.model;

import com.navigatingcancer.healthtracker.api.data.model.Adherence;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
public class AdherenceParserTest {

    @Test
    public void testAdherenceMedicationTaken() {
        AdherenceParser parser = new AdherenceParser();

        List<SurveyItemPayload> orals = new ArrayList<>();
        SurveyItemPayload itemPayload = new SurveyItemPayload();
        Map<String, Object> payload = new HashMap<>();
        payload.put("medicationTaken", "yes");
        itemPayload.setPayload(payload);
        orals.add(itemPayload);

        List<Adherence> parsedAdherences = parser.parse(orals, "unknonwn");

        Assert.assertNotNull(parsedAdherences);
        Assert.assertTrue(parsedAdherences.size() == 1);
        Assert.assertTrue("TAKEN".equalsIgnoreCase(parsedAdherences.get(0).getStatus()));
    }

    @Test
    public void testAdherenceMedicationSkipReason() {
        AdherenceParser parser = new AdherenceParser();

        List<SurveyItemPayload> orals = new ArrayList<>();
        SurveyItemPayload itemPayload = new SurveyItemPayload();
        Map<String, Object> payload = new HashMap<>();
        payload.put("medicationSkipReason", "outOfMedication");
        itemPayload.setPayload(payload);
        orals.add(itemPayload);

        List<Adherence> parsedAdherences = parser.parse(orals, "unknonwn");

        Assert.assertNotNull(parsedAdherences);
        Assert.assertTrue(parsedAdherences.size() == 1);
        Assert.assertTrue("I'm out of medication".equalsIgnoreCase(parsedAdherences.get(0).getReason()));
    }

    @Test
    public void testAdherenceMedicationStarted() {
        // Arrange
        AdherenceParser parser = new AdherenceParser();

        List<SurveyItemPayload> orals = new ArrayList<>();
        SurveyItemPayload itemPayload = new SurveyItemPayload();
        Map<String, Object> payload = new HashMap<>();
        String startDate = LocalDate.now().toString();
        payload.put("medicationStarted", "yes");
        payload.put("medicationStartedDate", startDate);
        itemPayload.setPayload(payload);
        orals.add(itemPayload);

        // Act
        List<Adherence> parsedAdherences = parser.parse(orals, "unknonwn");

        // Assert
        Assert.assertNotNull(parsedAdherences);
        Assert.assertTrue(parsedAdherences.size() == 1);
        Assert.assertTrue(AdherenceParser.MEDICATION_STARTED_YES.equalsIgnoreCase(parsedAdherences.get(0).getStatus()));
        Assert.assertEquals(startDate, parsedAdherences.get(0).getPatientReportedStartDate());
    }

    @Test
    public void testAdherenceMedicationNotStarted() {
        // Arrange
        AdherenceParser parser = new AdherenceParser();

        List<SurveyItemPayload> orals = new ArrayList<>();
        SurveyItemPayload itemPayload = new SurveyItemPayload();
        Map<String, Object> payload = new HashMap<>();
        payload.put("medicationStarted", "no");
        itemPayload.setPayload(payload);
        orals.add(itemPayload);

        // Act
        List<Adherence> parsedAdherences = parser.parse(orals, "unknonwn");

        // Assert
        Assert.assertNotNull(parsedAdherences);
        Assert.assertTrue(parsedAdherences.size() == 1);
        Assert.assertTrue(AdherenceParser.MEDICATION_STARTED_NO.equalsIgnoreCase(parsedAdherences.get(0).getStatus()));
    }
}
