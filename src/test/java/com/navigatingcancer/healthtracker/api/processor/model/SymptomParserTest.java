package com.navigatingcancer.healthtracker.api.processor.model;

import com.google.common.collect.ImmutableMap;
import com.navigatingcancer.healthtracker.api.data.model.SideEffect;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SymptomParserTest {

    @Test
    public void getParsedSideEffects() {
        boolean isProCtcae = false;

        final Map<String, Object> otherSymptom1 = ImmutableMap.<String, Object>builder()
                .put("otherComment", "Other symptom 1")
                .put("otherInterference", "3")
                .put("otherSeverity", "2")
                .build();

        final List<Map<String, Object>> otherSymptoms = List.of(otherSymptom1);

        final Map<String, Object> payloadMap = ImmutableMap.<String, Object>builder()
            .put("coughInterference", "3")
            .put("coughSeverity", "3")
            .put("numbnessTinglingComment", "hands")
            .put("numbnessTinglingInterference", "severe")
            .put("numbnessTinglingSeverity", "Severe")
            .put("numbnessortinglingComment", "hands")
            .put("numbnessortinglingInterference", "severe")
            .put("numbnessortinglingSeverity", "Severe")
            .put("painComment", "hands and feet")
            .put("painFrequency", "4")
            .put("painInterference", "3")
            .put("painSeverity", "8")
            .put("swellingComment", "hands")
            .put("swellingFrequency", "3")
            .put("swellingInterference", "3")
            .put("swellingSeverity", "1")
            .put("other", otherSymptoms)
            .build();


        List<SurveyItemPayload> inputList = new ArrayList<>();

        SurveyItemPayload surveyItemPayload = new SurveyItemPayload();
        surveyItemPayload.setPayload(payloadMap);
        inputList.add(surveyItemPayload);
        List<SideEffect> results = SymptomParser.parseIntoSideEffects(isProCtcae, inputList);

        Assert.assertEquals("correct number of parsed symptoms", 6, results.size());

        results.forEach(result -> {
            switch(result.getSymptomType().toLowerCase()) {
                case "cough": {
                    Assert.assertNull(String.format("%s-%s", result.getSymptomType(), "frequency"), result.getFrequency());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "severity"), "Severe", result.getSeverity());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "interference"), "Quite a bit", result.getInterference());
                    Assert.assertNull(String.format("%s-%s", result.getSymptomType(), "location"), result.getLocation());
                    break;
                }
                case "numbnessTingling": {
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "frequency"), "severe", result.getFrequency());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "severity"), "Severe", result.getSeverity());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "interference"), "severe", result.getInterference());
                    break;
                }
                case "numbnessortingling": {
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "severity"), "Severe", result.getSeverity());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "interference"), "severe", result.getInterference());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "location"), "hands", result.getLocation());
                    break;
                }
                case "pain": {
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "frequency"), "Almost constantly", result.getFrequency());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "severity"), "Severe", result.getSeverity());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "interference"), "Quite a bit", result.getInterference());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "location"), "hands and feet", result.getLocation());
                    break;
                }
                case "swelling": {
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "frequency"), "Frequently", result.getFrequency());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "severity"), "Mild", result.getSeverity());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "interference"), "Quite a bit", result.getInterference());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "location"), "hands", result.getLocation());
                    break;
                }
                case "other": {
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "severity"), "Moderate", result.getSeverity());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "interference"), "Quite a bit", result.getInterference());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "location"), "Other symptom 1", result.getLocation());
                    break;
                }
                default:

            }
        });
    }

    @Test
    public void getParsedSideEffects_withProCtcaeAndNoAdverseSymptoms() {
        boolean isProCtcae = true;

        final Map<String, Object> payloadMap = ImmutableMap.<String, Object>builder()
                .put("nauseaFrequency", "0")
                .put("nauseaSeverity", "0")
                .put("constipationSeverity", "0")
                .put("painFrequency", "0")
                .put("painInterference", "0")
                .put("painSeverity", "0")
                .build();

        List<SurveyItemPayload> inputList = new ArrayList<>();

        SurveyItemPayload surveyItemPayload = new SurveyItemPayload();
        surveyItemPayload.setPayload(payloadMap);
        inputList.add(surveyItemPayload);
        List<SideEffect> results = SymptomParser.parseIntoSideEffects(isProCtcae, inputList);

        Assert.assertEquals("correct number of parsed symptoms", 3, results.size());

        results.forEach(result -> {
            switch(result.getSymptomType().toLowerCase()) {
                case "nausea": {
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "frequency"), "Never", result.getFrequency());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "severity"), "None", result.getSeverity());
                    break;
                }
                case "constipation": {
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "severity"), "None", result.getSeverity());
                    break;
                }
                case "pain": {
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "frequency"), "Never", result.getFrequency());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "severity"), "None", result.getSeverity());
                    Assert.assertEquals(String.format("%s-%s", result.getSymptomType(), "interference"), "Not at all", result.getInterference());
                    break;
                }
                default:
            }
        });
    }


    @Test
    public void getParsedSideEffects_emptyOrNullInput() {
        List<SurveyItemPayload> inputList = null;
        Assert.assertEquals("Empty input", 0, SymptomParser.parseIntoSideEffects(false, inputList).size());

        inputList = new ArrayList<>();
        Assert.assertEquals("Empty input", 0, SymptomParser.parseIntoSideEffects(false, inputList).size());
    }

    @Test
    public void getParsedSideEffects_invalidSymptom() {
        List<SurveyItemPayload> inputList = new ArrayList<>();
        SurveyItemPayload payload = new SurveyItemPayload();

        final Map<String, Object> payloadMap = ImmutableMap.<String, Object>builder()
            .put("invalidInterference", "1").build();

        payload.setPayload(payloadMap);

        Assert.assertEquals("Invalid symptom type", 0, SymptomParser.parseIntoSideEffects(false, inputList).size());
    }

}
