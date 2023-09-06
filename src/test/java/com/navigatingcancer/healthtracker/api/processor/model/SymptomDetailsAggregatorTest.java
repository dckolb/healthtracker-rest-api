package com.navigatingcancer.healthtracker.api.processor.model;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SymptomDetailsAggregatorTest {

    private static SurveyItemPayload createSurvey(String... items) {
        if(items != null && (items.length %2) != 0)
            throw new IllegalArgumentException("createSurvey needs even number of items.");

        SurveyItemPayload item = new SurveyItemPayload();
        item.setPayload(new HashMap<>());

        if(items == null)
            return item;

        for (int i = 0; i < items.length; i = i + 2) {
            item.getPayload().put(items[i], items[i + 1]);
        }
        return item;
    }

    private static SymptomDetails getSymptomDetailsFromList(List<SymptomDetails> list, String symptomType) {
        SymptomDetails symptomDetails =
                list.stream().filter(sd -> sd.getSymptomType().equalsIgnoreCase(symptomType)).findAny().orElse(null);
        Assert.assertNotNull(symptomDetails);
        return symptomDetails;
    }

    private static void compareSymptomDetails(List<SymptomDetails> list, String symptom, String severity, String comment,
                                              String title, List<String> detailList) {
        SymptomDetails sd = getSymptomDetailsFromList(list, symptom);
        Assert.assertEquals("Symptom type matches : ", symptom, sd.getSymptomType());
        Assert.assertEquals("Severity matches : ", severity, sd.getSeverity());
        Assert.assertEquals("Comment matches : ", comment, sd.getComment());
        Assert.assertEquals("Title matches : ", title, sd.getTitle());

        Assert.assertEquals("details list matches", detailList, sd.getDetailList());
    }

    @Test
    public void getSymptomDetails_withNoSideEffectsReported() {
        SurveyItemPayload payload = createSurvey(null);
        List<SurveyItemPayload> inputList = new ArrayList<>();
        inputList.add(payload);

        SymptomDetailsAggregator aggregator = new SymptomDetailsAggregator(inputList, false);

        List<SymptomDetails> results = aggregator.getSymptomDetails();

        Assert.assertEquals("creates one symptom detail", 1, results.size());

        SymptomDetails symptomDetails = results.get(0);
        Assert.assertEquals("Symptom type is empty", "", symptomDetails.getSymptomType());
        Assert.assertEquals("Severity type is none", "none", symptomDetails.getSeverity());
        Assert.assertEquals("Comment is null", null, symptomDetails.getComment());
        Assert.assertEquals("Title is 'No side effects'", "No side effects", symptomDetails.getTitle());
        Integer expectedRawSeverity = 0;
        Assert.assertEquals("Raw severity is 0", expectedRawSeverity, symptomDetails.getRawSeverity());
    }

   @Test
    public void getSymptomDetails_withComplexPayload_forMN() {
        SurveyItemPayload payload = createSurvey(
                "painFrequency", "4", "painSeverity", "3", "painInterference", "1",
                "nauseaFrequency", "1", "nauseaSeverity", "0",
                "constipationSeverity", "0",
                "coughInterference", "3", "coughSeverity", "2",
                "diarrheaFrequency", "1", "diarrheaSeverity", "3",
                "fatigueSeverity", "2", "fatigueInterference", "3",
                "feverSeverity", "2", "feverComment", "100.12",
                "numbnessortinglingComment", "hands and feet", "numbnessortinglingSeverity", "2", "numbnessortinglingInterference", "4",
                "rashComment", "feet", "rashSeverity", "2",
                "swellingComment", "feet", "swellingFrequency", "1", "swellingSeverity", "3", "swellingInterference", "1",
                "shortnessofbreathSeverity", "1", "shortnessofbreathInterference", "1",
                "otherComment", "joint pain", "otherSeverity", "4", "otherInterference", "4"
        );
        List<SurveyItemPayload> inputList = new ArrayList<>();
        inputList.add(payload);

        //MN
        Boolean followsCtcaeStandard = true;

        SymptomDetailsAggregator aggregator = new SymptomDetailsAggregator(inputList, followsCtcaeStandard);

        List<SymptomDetails> results = aggregator.getSymptomDetails();

        Assert.assertEquals("creates all symptom details", 12, results.size());

        compareSymptomDetails(results, "constipation", "None", null, "No constipation",
                Arrays.asList(new String[]{}));
        compareSymptomDetails(results, "rash", "Moderate", "feet", "Moderate rash",
                Arrays.asList(new String[]{}));
        compareSymptomDetails(results, "pain", "Severe", null, "Severe pain",
                Arrays.asList("It interferes with my daily activities a little bit", "It occurs almost constantly"));
        compareSymptomDetails(results, "other", "Very severe", "joint pain", "Very severe other symptoms",
                Arrays.asList("It interferes with my daily activities very much"));
        compareSymptomDetails(results, "fatigue", "Moderate", null, "Moderate fatigue",
                Arrays.asList("It interferes with my daily activities quite a bit"));
        compareSymptomDetails(results, "numbnessortingling", "Moderate", "hands and feet", "Moderate numbness or tingling",
                Arrays.asList("It interferes with my daily activities very much"));
        compareSymptomDetails(results, "fever", "Moderate", "100.12", "Moderate fever",
                Arrays.asList(new String[]{}));
        compareSymptomDetails(results, "shortnessofbreath", "Mild", null, "Mild shortness of breath",
                Arrays.asList("It interferes with my daily activities a little bit"));
        compareSymptomDetails(results, "swelling", "Severe", "feet", "Severe swelling",
                Arrays.asList("It interferes with my daily activities a little bit", "It occurs rarely"));
        compareSymptomDetails(results, "cough", "Moderate", null, "Moderate cough",
                Arrays.asList("It interferes with my daily activities quite a bit"));
        compareSymptomDetails(results, "diarrhea", "Severe", null, "Severe diarrhea",
                Arrays.asList("It occurs rarely"));
        compareSymptomDetails(results, "nausea", "None", null, "No nausea",
                Arrays.asList("It occurs rarely"));
    }

    @Test
    public void getSymptomDetails_SkipSymptomDetails_forMN() {
        SurveyItemPayload payload = createSurvey(
                "painFrequency", "0", "painSeverity", "0", "painInterference", "0", "painComment", "",
                "nauseaFrequency", "0", "nauseaSeverity", "0",
                "constipationSeverity", "0"
        );
        List<SurveyItemPayload> inputList = new ArrayList<>();
        inputList.add(payload);

        //MN
        Boolean followsCtcaeStandard = true;

        SymptomDetailsAggregator aggregator = new SymptomDetailsAggregator(inputList, followsCtcaeStandard);

        List<SymptomDetails> results = aggregator.getSymptomDetails();

        Assert.assertEquals("creates all symptom details", 3, results.size());

        compareSymptomDetails(results, "pain", "None", null, "No pain",
                Arrays.asList(new String[]{}));
        compareSymptomDetails(results, "nausea", "None", null, "No nausea",
                Arrays.asList(new String[]{}));
        compareSymptomDetails(results, "constipation", "None", null, "No constipation",
                Arrays.asList(new String[]{}));
    }

    @Test
    public void getSymptomDetails_withComplexPayload_forNonMN() {
        SurveyItemPayload payload = createSurvey(
                // pain severity is set to 9 instead of 3
                "painFrequency", "4", "painSeverity", "9", "painInterference", "1", "painComment", "in a lot of pain",
                "nauseaFrequency", "1", "nauseaSeverity", "0",
                "coughInterference", "3", "coughSeverity", "2",
                "diarrheaFrequency", "1", "diarrheaSeverity", "3",
                "fatigueSeverity", "2", "fatigueInterference", "3",
                "feverSeverity", "2", "feverComment", "100.12",
                "numbnessortinglingComment", "hands and feet", "numbnessortinglingSeverity", "2", "numbnessortinglingInterference", "4",
                "rashComment", "feet", "rashSeverity", "2",
                "urinaryproblemsComment", "painful", "urinaryproblemsSeverity", "2",
                "swellingComment", "feet", "swellingFrequency", "1", "swellingSeverity", "3", "swellingInterference", "1",
                "shortnessofbreathSeverity", "1", "shortnessofbreathInterference", "1",
                "otherComment", "joint pain", "otherSeverity", "4", "otherInterference", "4"
        );

        List<SurveyItemPayload> inputList = new ArrayList<>();
        inputList.add(payload);

        //  Non MN
        Boolean followsCtcaeStandard = false;

        SymptomDetailsAggregator aggregator = new SymptomDetailsAggregator(inputList, followsCtcaeStandard);

        List<SymptomDetails> results = aggregator.getSymptomDetails();

        Assert.assertEquals("creates all symptom details", 12, results.size());

        compareSymptomDetails(results, "rash", "Moderate", "feet", "Moderate rash",
                Arrays.asList(new String[]{}));
        compareSymptomDetails(results, "pain", "Very severe", "in a lot of pain", "Very severe pain (9)",
                Arrays.asList("It interferes with my daily activities a little bit", "It occurs almost constantly"));
        compareSymptomDetails(results, "other", "Very severe", "joint pain", "Very severe other symptoms",
                Arrays.asList("It interferes with my daily activities very much"));
        compareSymptomDetails(results, "fatigue", "Moderate", null, "Moderate fatigue",
                Arrays.asList("It interferes with my daily activities quite a bit"));
        compareSymptomDetails(results, "numbnessortingling", "Moderate", "hands and feet", "Moderate numbness or tingling",
                Arrays.asList("It interferes with my daily activities very much"));
        compareSymptomDetails(results, "fever", "Moderate", "100.12", "Moderate fever",
                Arrays.asList(new String[]{}));
        compareSymptomDetails(results, "shortnessofbreath", "Mild", null, "Mild shortness of breath",
                Arrays.asList("It interferes with my daily activities a little bit"));
        compareSymptomDetails(results, "swelling", "Severe", "feet", "Severe swelling",
                Arrays.asList("It interferes with my daily activities a little bit", "It occurs rarely"));
        compareSymptomDetails(results, "cough", "Moderate", null, "Moderate cough",
                Arrays.asList("It interferes with my daily activities quite a bit"));
        compareSymptomDetails(results, "diarrhea", "Severe", null, "Severe diarrhea",
                Arrays.asList("It occurs rarely"));
        compareSymptomDetails(results, "nausea", "None", null, "No nausea",
                Arrays.asList("It occurs rarely"));
        compareSymptomDetails(results, "urinaryproblems", "Moderate", "painful", "Moderate urinary problems",
                new ArrayList<>());
    }

    @Test
    public void testSortedSymptomDetailResults() {
        SurveyItemPayload payload = createSurvey("constipationSeverity", "4",
                "coughSeverity", "3", "coughInterference", "1",
                "painComment", "pain comment 12345", "painSeverity", "3", "painFrequency", "1", "painInterference", "3",
                "feverSeverity", "2", "feverComment", "moderate fever",
                "nauseaFrequency", "1", "nauseaSeverity", "1");

        List<SurveyItemPayload> inputList = new ArrayList<>();
        inputList.add(payload);

        //  Non MN
        Boolean followsCtcaeStandard = false;

        SymptomDetailsAggregator aggregator = new SymptomDetailsAggregator(inputList, followsCtcaeStandard);

        List<SymptomDetails> results = aggregator.getSymptomDetails();

        Assert.assertEquals("creates all symptom details", 5, results.size());
        Assert.assertEquals("Very severe", results.get(0).getSeverity());
        Assert.assertEquals("Severe", results.get(1).getSeverity());
        Assert.assertEquals("Moderate", results.get(2).getSeverity());
        Assert.assertEquals("Mild", results.get(3).getSeverity());
        Assert.assertEquals("Mild", results.get(4).getSeverity());
    }
}
