package com.navigatingcancer.healthtracker.api.processor.model;

import org.junit.Assert;
import org.junit.Test;

public class SurveyDictionaryTest {

    @Test
    public void medicationSkipDescription() {
        Assert.assertEquals("out of medication",
                "I'm out of medication",
                SurveyDictionary.medicationSkipDescription("outOfMedication", "Other"));

        Assert.assertEquals("other",
                "Other",
                SurveyDictionary.medicationSkipDescription("other", "Other"));

        Assert.assertEquals("foo bar",
                "Other",
                SurveyDictionary.medicationSkipDescription("foo bar", "Other"));
    }

    @Test
    public void interferenceDescription() {
        String invalidSurveyType = "this is ignored";

        Assert.assertEquals("interference level 1",
                "A little bit",
                SurveyDictionary.interferenceDescription(invalidSurveyType, "1"));

        Assert.assertEquals("interference level 2",
                "Somewhat",
                SurveyDictionary.interferenceDescription(invalidSurveyType, "2"));

        Assert.assertEquals("interference level 3",
                "Quite a bit",
                SurveyDictionary.interferenceDescription(invalidSurveyType, "3"));

        Assert.assertEquals("interference level 4",
                "Very much",
                SurveyDictionary.interferenceDescription(invalidSurveyType, "4"));
    }

    @Test
    public void interferenceDescription_forProCTCAE() {
        String invalidSurveyType = "this is ignored";

        Assert.assertEquals("PRO-CTCAE interference level 0",
                "Not at all",
                SurveyDictionary.interferenceDescription(invalidSurveyType, "0"));

        Assert.assertEquals("PRO-CTCAE interference level 1",
                "A little bit",
                SurveyDictionary.interferenceDescription(invalidSurveyType, "1"));
    }

    @Test
    public void frequencyDescription_forSwelling() {
        String surveyType = "swelling";

        Assert.assertEquals("For Swelling : frequency level 4",
                "Constantly",
                SurveyDictionary.frequencyDescription(false, surveyType, "4"));
    }

    @Test
    public void frequencyDescription_forAllOthers() {
        String surveyType = "any other type besides swelling";

        Assert.assertEquals("For any other symptom type : frequency level 4",
                "Almost constantly",
                SurveyDictionary.frequencyDescription(false, surveyType, "4"));
    }

    @Test
    public void frequencyDescription_forProCTCAE() {
        String surveyType = "dont care";

        Assert.assertEquals("For PRO-CTCAE : frequency level 0",
                "Never",
                SurveyDictionary.frequencyDescription(true, surveyType, "0"));

        Assert.assertEquals("For PRO-CTCAE : frequency level 1",
                "Rarely",
                SurveyDictionary.frequencyDescription(true, surveyType, "1"));
    }

    @Test
    public void severityDescription_forPain() {
        String symptomType = "pain";

        Assert.assertEquals("For pain : severity  level 1",
                "Mild",
                SurveyDictionary.severityDescription(false, symptomType, "1"));

        Assert.assertEquals("For pain : severity  level 3",
                "Mild",
                SurveyDictionary.severityDescription(false, symptomType, "3"));

        Assert.assertEquals("For pain : severity  level 4",
                "Moderate",
                SurveyDictionary.severityDescription(false, symptomType, "4"));

        Assert.assertEquals("For pain : severity  level 7",
                "Severe",
                SurveyDictionary.severityDescription(false, symptomType, "7"));

        Assert.assertEquals("For pain : severity  level 9",
                "Very severe",
                SurveyDictionary.severityDescription(false, symptomType, "9"));

        Assert.assertEquals("For pain : severity  level invalid",
                "invalid",
                SurveyDictionary.severityDescription(false, symptomType, "invalid"));

    }

    // HT-559
    @Test
    public void severityDescription_forFever() {
        String symptomType = "fever";

        Assert.assertEquals("For fever : severity  level 1",
                "Mild",
                SurveyDictionary.severityDescription(false, symptomType, "1"));

        Assert.assertEquals("For fever : severity  level 2",
                "Moderate",
                SurveyDictionary.severityDescription(false, symptomType, "2"));

        Assert.assertEquals("For fever : severity  level 3",
                "Severe",
                SurveyDictionary.severityDescription(false, symptomType, "3"));

        Assert.assertEquals("For fever : severity  level 4",
                "Very severe",
                SurveyDictionary.severityDescription(false, symptomType, "4"));

         Assert.assertEquals("For fever : severity  level invalid",
                 "invalid",
                 SurveyDictionary.severityDescription(false, symptomType, "invalid"));
     }

    @Test
    public void severityDescription_forOthers() {
        String surveyType = "anything else";

        Assert.assertEquals("For all others : severity level 1",
                "Mild",
                SurveyDictionary.severityDescription(false, surveyType, "1"));

        Assert.assertEquals("For all others : severity  level 2",
                "Moderate",
                SurveyDictionary.severityDescription(false, surveyType, "2"));

        Assert.assertEquals("For all others : severity  level 3",
                "Severe",
                SurveyDictionary.severityDescription(false, surveyType, "3"));

        Assert.assertEquals("For all others : severity  level 4",
                "Very severe",
                SurveyDictionary.severityDescription(false, surveyType, "4"));

         Assert.assertEquals("For all others : severity  level invalid",
                 "invalid",
                 SurveyDictionary.severityDescription(false, surveyType, "invalid"));
     }

    @Test
    public void severityDescription_forProCtcae() {
        String surveyType = "anything else";

        Assert.assertEquals("For PRO-CTCAE : severity  level 0",
                "None",
                SurveyDictionary.severityDescription(false, surveyType, "0"));

        Assert.assertEquals("For PRO-CTCAE : severity  level 1",
                "Mild",
                SurveyDictionary.severityDescription(false, surveyType, "1"));

        Assert.assertEquals("For PRO-CTCAE : severity  level 2",
                "Moderate",
                SurveyDictionary.severityDescription(false, surveyType, "2"));

        Assert.assertEquals("For PRO-CTCAE : severity  level 3",
                "Severe",
                SurveyDictionary.severityDescription(false, surveyType, "3"));

        Assert.assertEquals("For PRO-CTCAE : severity  level 4",
                "Very severe",
                SurveyDictionary.severityDescription(false, surveyType, "4"));
    }

    @Test
    public void descriptiveTitle() {
        Assert.assertEquals("DescriptiveTitle: pain severity", "Very severe pain",
                SurveyDictionary.descriptiveTitle(Symptom.FSIL_SEVERITY, "Very severe", "pain"));

        Assert.assertEquals("DescriptiveTitle: mtsores severity", "Very severe mouth or throat sores",
                SurveyDictionary.descriptiveTitle(Symptom.FSIL_SEVERITY, "Very severe", "mtsores"));

        Assert.assertEquals("DescriptiveTitle: foo bar frequency", "Foo bar occurs all the time",
                SurveyDictionary.descriptiveTitle(Symptom.FSIL_FREQUENCY, "All the time", "foo bar"));

        Assert.assertEquals("DescriptiveTitle: foo bar interference ", "Interferes with my daily life daily",
                SurveyDictionary.descriptiveTitle(Symptom.FSIL_INTERFERENCE, "Daily", "foo bar"));

    }
}
