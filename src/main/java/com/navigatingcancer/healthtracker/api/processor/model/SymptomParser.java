package com.navigatingcancer.healthtracker.api.processor.model;

import com.navigatingcancer.healthtracker.api.data.model.SideEffect;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.navigatingcancer.healthtracker.api.processor.model.Symptom.SYMPTOM_TYPES;

@Slf4j
public class SymptomParser {
    public static final String OTHER_KEY = "other";

    /* Example payload
       "symptoms": [
       {
         "_id": "5d4a2a733d4fc400017d3a95",
         "payload": {
           "coughInterference": "3",
           "coughSeverity": "3",
           "numbnessortinglingComment": "hands",
           "numbnessortinglingInterference": "3",
           "numbnessortinglingSeverity": "3",
           "painComment": "hands",
           "painFrequency": "4",
           "painInterference": "3",
           "painSeverity": "7",
           "swellingComment": "hands",
           "swellingFrequency": "3",
           "swellingInterference": "3",
           "swellingSeverity": "3"
         }
       }
     ]
     */

    public static List<SideEffect> parseIntoSideEffects(boolean isProCtcaeFormat, List<SurveyItemPayload> payloadSymptoms) {
        return createSideEffectsFromSymptoms(parseIntoSymptoms(isProCtcaeFormat, payloadSymptoms));
    }

    public static List<Symptom> parseIntoSymptoms(boolean isProCtcaeFormat, List<SurveyItemPayload> payloadSymptoms) {
        Map<String, Symptom> symptomDetailsBySurveyType  = new HashMap<>();
        List<Symptom> symptoms = new ArrayList<>();

        if (payloadSymptoms != null) {
            payloadSymptoms.forEach(payloadSymptom -> {
                payloadSymptom.getPayload().forEach((surveyTypeWithFSIL, value) -> {
                    if (value instanceof String) {
                        String valueString = (String) value;
                        parseEntry(isProCtcaeFormat, surveyTypeWithFSIL, valueString, symptomDetailsBySurveyType);
                        return;
                    }
                    if (value instanceof List) {
                        List<Object> symptomMaps = (List<Object>) value;
                        symptomMaps.forEach((symptomMap) -> {
                            Map<String,String> map = (Map<String, String>) symptomMap;
                            symptoms.addAll(parseMap(isProCtcaeFormat, map));
                        });
                        return;
                    }
                    log.error("Can't handle payload value: {}", value);
                    log.error("value type: {}", value.getClass().getName());
                    throw new RuntimeException(String.format("Don't know how to handle payload value with type: %s", value.getClass().getName()));
                });
            });
        }

        symptoms.addAll(symptomDetailsBySurveyType.values());

        return symptoms;
    }

    private static Collection<Symptom> parseMap(boolean isProCtcaeFormat, Map<String, String> symptomMap) {
        Map<String, Symptom> symptomDetails = new HashMap<>();
        symptomMap.forEach((key, value) -> {
            parseEntry(isProCtcaeFormat, key, value, symptomDetails);
        });
        return symptomDetails.values();
    }

    private static void parseEntry(boolean isProCtcaeFormat, String key, String value, Map<String, Symptom> details) {
        // Example surveyTypeWithFSIL : numbnessortinglingInterference
        String[] parsedKeys = parseSurveyKey(key);

        String symptomType = parsedKeys[0]; //Example cough, numbnessortinglingI
        String fsilType = parsedKeys[1]; // These are the FSIL types

        if(Symptom.valid(symptomType)) {
            Symptom symptom = null;
            if(details.containsKey(symptomType)) {
                symptom = details.get(symptomType);
            } else {
                symptom = new Symptom(symptomType, isProCtcaeFormat);
                details.put(symptomType, symptom);
            }

            // { 'cough' => CoughSymptomObject, 'swelling' => SwellingSymptomObject }
            symptom.setSymptomAttribute(fsilType.toLowerCase(), value);

        } else {
            log.error("Invalid / new SymptomType {} specified in payload", symptomType);
        }

    }

    private static List<SideEffect> createSideEffectsFromSymptoms(List<Symptom> symptoms) {
        return symptoms.stream().map(Symptom::toSideEffect).collect(Collectors.toList());
    }

    private static String[] parseSurveyKey(final String key) {
        String[] results = new String[2];
        for (String surveyType: SYMPTOM_TYPES) {
            if(key.startsWith(surveyType)) {
                if(surveyType.equals("urinary") && key.startsWith("urinaryproblems")) {
                    surveyType = "urinaryproblems";
                }
                results[0] = surveyType;
                results[1] = key.substring(surveyType.length()).toUpperCase();
                break;
            }
        }

        return results;
    }

}
