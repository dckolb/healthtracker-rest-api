package com.navigatingcancer.healthtracker.api.processor.model;

import com.google.common.collect.ImmutableSet;
import com.navigatingcancer.healthtracker.api.data.model.SideEffect;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Setter(AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode(of = {"symptomType", "frequency", "severity", "interference", "comment", "occurrence", "rawSeverity"})
@Slf4j
public class Symptom {

    public static final String FSIL_COMMENT = "COMMENT";
    public static final String FSIL_INTERFERENCE = "INTERFERENCE";
    public static final String FSIL_SEVERITY = "SEVERITY";
    public static final String FSIL_FREQUENCY = "FREQUENCY";
    public static final String FSIL_OCCURRENCE = "OCCURRENCE";

    public static final Set<String> FSIL_TYPES = new HashSet<String>() {{
        add(FSIL_FREQUENCY);
        add(FSIL_SEVERITY);
        add(FSIL_INTERFERENCE);
        add(FSIL_COMMENT);
        add(FSIL_OCCURRENCE);
    }};

    public static final ImmutableSet<String> SYMPTOM_TYPES = ImmutableSet.copyOf(
            Arrays.asList(
                    "abdominalPain",
                    "activityFunction",
                    "blurredvision",
                    "chills",
                    "constipation",
                    "cough",
                    "diarrhea",
                    "emergencyRoom",
                    "fatigue",
                    "fever",
                    "generalPain",
                    "headache",
                    "hospitalization",
                    "jointPain",
                    // "medicationSkipReason",
                    "mtsores", // mouth or throat sores
                    "mouthorthroatsores",
                    "musclePain",
                    "nausea",
                    "numbnessTingling",
                    "numbnessortingling", // TODO: track down where this legacy value is still being passed in
                    "other",
                    "pain",
                    "rash",
                    "sob",
                    "shortnessofbreath",
                    "swelling",
                    "urinary",
                    "urinaryproblems",
                    "visualFloaters",
                    "vomiting"
            ));

    // FIXME should be enum ??
    private String symptomType;
    private boolean isProCtcaeFormat;

    private String frequency;
    private String severity;
    private String interference;
    private String comment;
    private String rawSeverity; // these are survey answers like "4", "9"
    private String rawInterference;
    private String rawFrequency;
    private String occurrence;

    public Symptom(final String symptomType, boolean isProCtcaeFormat) {
        if(!valid(symptomType)) {
            log.error("%s symptom type is not supported.", symptomType);
        }
        this.symptomType = symptomType;
        this.isProCtcaeFormat = isProCtcaeFormat;
    }

    private static Pattern PRO_CTCAE_SYMPTOMS_MATCHER = Pattern.compile("^(.*)(Frequency|Interference|Severity|Comment){1}$");

    // Input : painFrequency
    // Output : [ pain, FREQUENCY ]
    public static String[] parseSymptomStringFromPayload(String str) {
        String[] result =  new String[2];
        Matcher matcher = null;

        matcher = PRO_CTCAE_SYMPTOMS_MATCHER.matcher(str);
        if(matcher.find()) {
            result[0] = matcher.group(1);
            result[1] = matcher.group(2).toUpperCase();
        }

        return result;
    }

    public static boolean valid(String symptomType) {
        return SYMPTOM_TYPES.contains(symptomType);
    }

    public void setSymptomAttribute(String fsilType, String valueString) {
        switch (fsilType.toUpperCase()) {
            case FSIL_COMMENT:
                setComment(valueString);
                break;
            case FSIL_INTERFERENCE:
                setInterference(SurveyDictionary.interferenceDescription(symptomType, valueString));
                setRawInterference(valueString);
                break;
            case FSIL_SEVERITY:
                setSeverity(SurveyDictionary.severityDescription(isProCtcaeFormat, symptomType, valueString));
                setRawSeverity(valueString);
                break;
            case FSIL_FREQUENCY:
                setFrequency(SurveyDictionary.frequencyDescription(isProCtcaeFormat, symptomType, valueString));
                setRawFrequency(valueString);
                break;
            case FSIL_OCCURRENCE:
                setOccurrence(SurveyDictionary.occurrenceDescription(symptomType, valueString));
                break;
            default:
                log.error("%s type is not supported.", fsilType);
        }
    }

    public SideEffect toSideEffect() {
        SideEffect sideEffect = new SideEffect();

        sideEffect.setSymptomType(symptomType);

        sideEffect.setFrequency(frequency);
        sideEffect.setSeverity(severity);
        sideEffect.setInterference(interference);
        sideEffect.setLocation(comment);
        sideEffect.setOccurrence(occurrence);
        sideEffect.setRawSeverity(rawSeverity);

        return sideEffect;
    }
}
