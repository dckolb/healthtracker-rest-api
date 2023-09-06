package com.navigatingcancer.healthtracker.api.processor.model;

import com.google.common.collect.ImmutableMap;
import org.codehaus.plexus.util.StringUtils;

import java.util.Map;

public class SurveyDictionary {

    private static final Map<String, String> OCCURRENCE_MAPPINGS
            = ImmutableMap.of(
            "yes", "Has had an episode in the last 7 days",
            "no", "Has not had an episode in the last 7 days"
    );

    private static final Map<String, String> INTERFERENCE_MAPPINGS
            = ImmutableMap.of(
            "0", "Not at all",
            "1", "A little bit",
            "2", "Somewhat",
            "3", "Quite a bit",
            "4", "Very much"
    );

    private static final Map<String, String> FREQUENCY_MAPPINGS_WITH_ALMOST
            = ImmutableMap.of(
            "0", "Never",
            "1", "Rarely",
            "2", "Occasionally",
            "3", "Frequently",
            "4", "Almost constantly"
    );

    private static final Map<String, String> FREQUENCY_MAPPINGS_NUMERICAL
        = ImmutableMap.of("4", "4+");

    private static final Map<String, String> FREQUENCY_MAPPINGS
            = ImmutableMap.of(
            "1", "Rarely",
            "2", "Occasionally",
            "3", "Frequently",
            "4", "Constantly"
    );

    private static final Map<String, String> SEVERITY_SCALE_5_MAPPINGS
            = ImmutableMap.of(
            "0", "None",
            "1", "Mild",
            "2", "Moderate",
            "3", "Severe",
            "4", "Very severe"
    );

    // 2019-08-21  HT-559
    // GC looking for Mild,Moderate,Sever,Very Severe only, removing clauses
    private static final Map<String, String> SEVERITY_FEVER
            = ImmutableMap.of(
            "1", "Mild",
            "2", "Moderate",
            "3", "Severe",
            "4", "Very severe"
    );

    private static  final Map<String, String> SEVERITY_SCALE_10_MAPPINGS = ImmutableMap.<String, String>builder()
            .put("1", "Mild")
            .put("2", "Mild")
            .put("3", "Mild")
            .put("4", "Moderate")
            .put("5", "Moderate")
            .put("6", "Moderate")
            .put("7", "Severe")
            .put("8", "Severe")
            .put("9", "Very severe")
            .put("10", "Very severe")
            .build();

    private static  final Map<String, String> MEDICATION_SKIP_MAPPINGS = ImmutableMap.<String, String>builder()
            .put("forgot", "I forgot to take my medication")
            .put("outOfMedication", "I'm out of medication")
            .put("notSupposedTo", "I don't think I'm supposed to")
            .put("notFeelingWell", "I'm not feeling well")
            .put("paused", "My doctor paused my treatment")
            .put("financialConcern", "Financial concern")
            .put("other", "Other")
            .build();

    private static  final Map<String, String> ACTIVITY_FUNCTION_MAPPINGS = ImmutableMap.<String, String>builder()
            .put("0", "Normal with no limitations")
            .put("1", "Not my normal self, but able to be up and about with fairly normal activities")
            .put("2", "Not feeling up to most things, but in bed or chair less than half the day")
            .put("3", "Able to do little activity & spend most of the day in bed or chair")
            .put("4", "Pretty much bedridden, rarely out of bed")
            .build();

    private static  final Map<String, String> DESCRIPTIVE_SYMPTOM_TYPE = ImmutableMap.<String, String>builder()
            .put("abdominalPain", "abdominal pain")
            .put("activityFunction", "activity")
            .put("blurredvision", "blurred vision")
            .put("emergencyRoom", "Emergency Room Visits")
            .put("generalPain", "general pain")
            .put("hospitalization", "Overnight Hospital Visits")
            .put("jointPain", "joint pain")
            .put("medicationSkipReason", "reason for skipping medication")
            .put("mtsores", "mouth or throat sores")
            .put("mouthorthroatsores", "mouth or throat sores")
            .put("musclePain", "muscle pain")
            .put("numbnessTingling", "numbness or tingling")
            .put("numbnessortingling", "numbness or tingling")
            .put("shortnessofbreath", "shortness of breath")
            .put("urinaryproblems", "urinary problems")
            .put("visualFloaters", "visual floaters")
            .put("other", "other symptoms")
            .build();

    public static String descriptiveSymptomType(final String key) {
        if(StringUtils.isBlank(key))
            return key;
        return DESCRIPTIVE_SYMPTOM_TYPE.getOrDefault(key, key);
    }

    public static String medicationSkipDescription(final String key, final String defaultValue) {
        if(StringUtils.isBlank(key))
            return key;
        return MEDICATION_SKIP_MAPPINGS.getOrDefault(key, defaultValue);
    }

    // NOTE surveyType not consumed currently
    public static String interferenceDescription(final String surveyType, final String key) {
        if(StringUtils.isBlank(key))
            return key;
        if (surveyType.equalsIgnoreCase("activityFunction"))
            return ACTIVITY_FUNCTION_MAPPINGS.getOrDefault(key, key);
        return INTERFERENCE_MAPPINGS.getOrDefault(key, key);
    }

    public static String occurrenceDescription(final String surveyType, final String key) {
        if(StringUtils.isBlank(key))
            return key;
        return OCCURRENCE_MAPPINGS.getOrDefault(key, key);
    }

    public static String frequencyDescription(boolean isProCtcae, final String surveyType, final String key) {
        if(StringUtils.isBlank(key))
            return key;
        if (isProCtcae) {
            return FREQUENCY_MAPPINGS_WITH_ALMOST.getOrDefault(key, key);
        } else if (surveyType.equalsIgnoreCase("hospitalization") || surveyType.equalsIgnoreCase("emergencyRoom")) {
            return FREQUENCY_MAPPINGS_NUMERICAL.getOrDefault(key, key);
        } else if (StringUtils.isNotBlank(surveyType) && surveyType.equalsIgnoreCase("swelling")) {
            return FREQUENCY_MAPPINGS.getOrDefault(key, key);
        } else
            return FREQUENCY_MAPPINGS_WITH_ALMOST.getOrDefault(key, key);
    }

    public static String severityDescription(boolean isProCtcae, final String surveyType, final String key) {
        if(StringUtils.isBlank(key))
            return key;
        if (isProCtcae)
            return SEVERITY_SCALE_5_MAPPINGS.getOrDefault(key, key);
        else if (surveyType.equalsIgnoreCase("pain"))
            return SEVERITY_SCALE_10_MAPPINGS.getOrDefault(key, key);
        else if (surveyType.equalsIgnoreCase("fever"))
            return SEVERITY_FEVER.getOrDefault(key, key);
        else
            return SEVERITY_SCALE_5_MAPPINGS.getOrDefault(key, key);
    }

    public static String description(String fsilType, boolean isProCtcae, final String surveyType, final String key) {
        if(StringUtils.isBlank(key))
            return key;
        switch (fsilType.toUpperCase()) {
            case Symptom.FSIL_SEVERITY :
                return severityDescription(isProCtcae, surveyType, key);
            case Symptom.FSIL_FREQUENCY :
                return frequencyDescription(isProCtcae, surveyType, key);
            case Symptom.FSIL_INTERFERENCE :
                return interferenceDescription(surveyType, key);
            default:
                return key;
        }
    }


    public static String descriptiveTitle(String fsilType, String description, String parsedSymptom) {
        switch (fsilType.toUpperCase()) {
            case Symptom.FSIL_FREQUENCY :
                return String.format("%s occurs %s", StringUtils.capitalise(parsedSymptom), description.toLowerCase());
            case Symptom.FSIL_INTERFERENCE :
                return String.format("Interferes with my daily life %s", description.toLowerCase());
            default:
                String descriptiveSymptomType = descriptiveSymptomType(parsedSymptom);
                return String.format("%s %s", description, descriptiveSymptomType);
        }
    }
}
