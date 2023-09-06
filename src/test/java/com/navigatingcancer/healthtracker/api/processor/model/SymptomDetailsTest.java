package com.navigatingcancer.healthtracker.api.processor.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SymptomDetailsTest {

    @Test
    public void compareTo() {

        SymptomDetails mild = new SymptomDetails();
        mild.setSeverity("Mild");
        SymptomDetails moderate = new SymptomDetails();
        moderate.setSeverity("Moderate");
        SymptomDetails severe = new SymptomDetails();
        severe.setSeverity("Severe");
        SymptomDetails verySevere = new SymptomDetails();
        verySevere.setSeverity("Very severe");
        SymptomDetails invalid = new SymptomDetails();
        invalid.setSeverity("invalid severity");

        List<SymptomDetails> unsortedList = new ArrayList<>(Arrays.asList(severe, moderate, invalid, mild, verySevere));

        Assert.assertEquals("invalid < valid severity", -1, invalid.compareTo(mild));
        Assert.assertEquals("valid > valid severity", 1, moderate.compareTo(invalid));
        Assert.assertEquals("Equality works", 0, moderate.compareTo(moderate));
        Assert.assertEquals("Severe > moderate", 1, severe.compareTo(moderate));
        Assert.assertEquals("Moderate < severe", -1, moderate.compareTo(severe));
        Assert.assertEquals("Mild < severe", -1, mild.compareTo(severe));
        Assert.assertEquals("Mild < severe", 1, severe.compareTo(mild));

        Collections.sort(unsortedList, Collections.reverseOrder());

        Assert.assertEquals(verySevere, unsortedList.get(0));
        Assert.assertEquals(severe, unsortedList.get(1));
        Assert.assertEquals(moderate, unsortedList.get(2));
        Assert.assertEquals(mild, unsortedList.get(3));
        Assert.assertEquals(invalid, unsortedList.get(4));
    }
}