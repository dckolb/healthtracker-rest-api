package com.navigatingcancer.healthtracker.api.processor.model;

import com.navigatingcancer.healthtracker.api.data.model.SideEffect;

import org.junit.Assert;
import org.junit.Test;

public class SymptomTest {

    @Test
    public void toSideEffect() {
        Symptom symptom = new Symptom("fever", false);
        symptom.setSymptomAttribute("FREQUENCY", "3");
        symptom.setSymptomAttribute("INTERFERENCE", "3");
        symptom.setSymptomAttribute("SEVERITY", "3");
        symptom.setSymptomAttribute("COMMENT", "hands and feet");

        SideEffect se = symptom.toSideEffect();

        Assert.assertEquals("side effect - frequency", "Frequently", se.getFrequency());
        Assert.assertEquals("side effect - interference", "Quite a bit", se.getInterference());
        Assert.assertEquals("side effect - severity", "Severe", se.getSeverity());
        Assert.assertEquals("side effect - comment", "hands and feet", se.getLocation());
    }

     @Test
    public void toSideEffect_proCtcaeAndAllZeroesForFSI() {
        Symptom symptom = new Symptom("pain", true);
        symptom.setSymptomAttribute("FREQUENCY", "0");
        symptom.setSymptomAttribute("INTERFERENCE", "0");
        symptom.setSymptomAttribute("SEVERITY", "0");

        SideEffect se = symptom.toSideEffect();

        Assert.assertEquals("side effect - frequency", "Never", se.getFrequency());
        Assert.assertEquals("side effect - interference", "Not at all", se.getInterference());
        Assert.assertEquals("side effect - severity", "None", se.getSeverity());
    }

    @Test
    public void toSideEffect_proCtcaeAndSymptoms() {
        Symptom symptom = new Symptom("pain", true);
        symptom.setSymptomAttribute("FREQUENCY", "3");
        symptom.setSymptomAttribute("INTERFERENCE", "4");
        symptom.setSymptomAttribute("SEVERITY", "1");

        SideEffect se = symptom.toSideEffect();

        Assert.assertEquals("side effect - frequency", "Frequently", se.getFrequency());
        Assert.assertEquals("side effect - interference", "Very much", se.getInterference());
        Assert.assertEquals("side effect - severity", "Mild", se.getSeverity());
    }

    @Test
    public void valid() {
        Assert.assertTrue("With valid symptom type", Symptom.valid(Symptom.SYMPTOM_TYPES.iterator().next()));
        Assert.assertFalse("With invalid symptom type", Symptom.valid("invalid"));
    }

    @Test
    public void parseSymtomStringFromPayload() {
        String painFrequency = "painFrequency";

        String[] results = Symptom.parseSymptomStringFromPayload(painFrequency);
        Assert.assertEquals("parses painFrequency correctly", "pain", results[0]);
        Assert.assertEquals("parses painFrequency correctly", "FREQUENCY", results[1]);

        String painInterference = "painInterference";

        results = Symptom.parseSymptomStringFromPayload(painInterference);
        Assert.assertEquals("parses painInterference correctly", "pain", results[0]);
        Assert.assertEquals("parses painInteference correctly", "INTERFERENCE", results[1]);

        String painSeverity = "painSeverity";

        results = Symptom.parseSymptomStringFromPayload(painSeverity);
        Assert.assertEquals("parses painSeverity correctly", "pain", results[0]);
        Assert.assertEquals("parses painSeverity correctly", "SEVERITY", results[1]);

        String painComment = "painComment";

        results = Symptom.parseSymptomStringFromPayload(painComment);
        Assert.assertEquals("parses painComment correctly", "pain", results[0]);
        Assert.assertEquals("parses painComment correctly", "COMMENT", results[1]);
    }
}
