package com.navigatingcancer.healthtracker.api.processor.model;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@Slf4j
public class SymptomDetailsAggregator {
    @Getter
    private List<SymptomDetails> allSymptomDetails = new ArrayList<>();
    @Getter
    private List<SurveyItemPayload> surveyItemPayloads;
    private Boolean followsCtcaeStandard;

    /* sample input :
        "symptoms": [
          {
          "id": "5d530c334702aa000137c366",
          "payload": {
            "painFrequency": "4", "painSeverity": "3", "painInterference": "2",
            "nauseaFrequency": "1", "nauseaSeverity": "0",
            "constipationSeverity": "4",
            "coughSeverity": "2", "coughInterference": "3",
            "diarrheaFrequency": "1", "diarrheaSeverity": "3",
            "fatigueSeverity": "4", "fatigueInterference": "3",
            "feverSeverity": "2", "feverComment": "100.12",
            "numbnessortinglingComment": "hands and feet", "numbnessortinglingSeverity": "2", "numbnessortinglingInterference": "4",
            "rashComment": "feet", "rashSeverity": "2",
            "swellingComment": "feet", "swellingFrequency": "1", "swellingSeverity": "3", "swellingInterference": "1",
            "shortnessofbreathSeverity": "1", "shortnessofbreathInterference": "1",
            "otherComment": "joint pain", "otherSeverity": "4", "otherInterference": "4"
            }
          }
        ]
     */

    /* Sample output :
      "symptomDetails": [
        {
          "title": "Severe pain (9)",
          "detailList":[
                "It occurs almost constantly",
                "It interferes with my daily activities very much"
           ],
          "symptom": "pain",
          "severity": "severe",
        }
      ]
     */

    public SymptomDetailsAggregator(List<SurveyItemPayload> surveyItemPayloads, Boolean followsCtcaeStandard) {
        this.surveyItemPayloads = surveyItemPayloads;
        this.followsCtcaeStandard = followsCtcaeStandard;
    }

    public static SymptomDetails findOtherSymptomDetail(List<SymptomDetails> list) {
        if(list == null || list.isEmpty()) {
            return null;
        }
        Optional<SymptomDetails> otherSymptom =
                list.stream().filter(detail -> detail.getSymptomType().equalsIgnoreCase("other")).findFirst();

        return otherSymptom.orElse(null);
    }

    public List<SymptomDetails> getSymptomDetails() {
        log.debug("SymptomDetailsAggregator::getSymptomDetails");
        if (!allSymptomDetails.isEmpty()) {
            return allSymptomDetails;
        }

        if (surveyItemPayloads == null) {
            log.debug("SymptomDetails were empty");
            return allSymptomDetails;
        }

        if (surveyItemPayloads.size() == 1) {
            Map<String, Object> payload = surveyItemPayloads.get(0).getPayload();
            if (payload != null && payload.size() == 0) {
                log.debug("User reported that they had no symptoms");
                allSymptomDetails.add(SymptomDetails.createSymptomDetailsWithNoSymptoms());
                return allSymptomDetails;
            }
        }

        List<Symptom> symptoms = SymptomParser.parseIntoSymptoms(followsCtcaeStandard, surveyItemPayloads);

        log.debug("payload to parse : {}", surveyItemPayloads);
        for (Symptom symptom : symptoms) {
            SymptomDetails symptomDetails = new SymptomDetails();
            symptomDetails.setSymptomType(symptom.getSymptomType());
            symptomDetails.setSeverity(symptom.getSeverity());

            // setup Interference details
            log.debug("Add details for symptom : {}", symptom);
            if (!skipSymptomDetails(symptom.getSymptomType(), symptom.getRawInterference(), followsCtcaeStandard)) {
                String description = SurveyDictionary.interferenceDescription(symptom.getSymptomType(),
                        symptom.getInterference());
                if (symptom.getSymptomType().equalsIgnoreCase("activityFunction")) {
                    symptomDetails.getDetailList().add(description);
                } else {
                symptomDetails.getDetailList().add(String.format("It interferes with my daily activities %s",
                        description.toLowerCase()));
            }
            }

            // setup Frequency details
            if (!skipSymptomDetails(symptom.getSymptomType(), symptom.getRawFrequency(), followsCtcaeStandard)) {
                String description = SurveyDictionary.frequencyDescription(followsCtcaeStandard,
                        symptom.getSymptomType(), symptom.getFrequency());
                String times = description.equals("1") ? "time" : "times";
                if (symptom.getSymptomType().equalsIgnoreCase("emergencyRoom")) {
                    symptomDetails.getDetailList().add(String.format("I have visited the emergency room %1s %2s in the past 7 days", description, times));
                } else if (symptom.getSymptomType().equalsIgnoreCase("hospitalization")) {
                        symptomDetails.getDetailList().add(String.format("I have been hospitalized overnight %1s %2s in the past 7 days", description, times));
                } else {
                    symptomDetails.getDetailList().add(String.format("It occurs %s", description.toLowerCase()));
                }
            }

            if (StringUtils.isNotBlank(symptom.getComment())) {
                symptomDetails.setComment(symptom.getComment());
            }

            if (StringUtils.isNotBlank(symptom.getRawSeverity())) {
                int severity;
                try {
                    severity = Integer.parseInt(symptom.getRawSeverity());
                    symptomDetails.setRawSeverity(severity);
                } catch (NumberFormatException nfe) {
                    log.error("Invalid severity in payload {}", symptom.getSeverity());
                }
            }

            symptomDetails.updateTitle(followsCtcaeStandard);
            allSymptomDetails.add(symptomDetails);
        }

        log.debug("symptomDetails before sorting : {}", allSymptomDetails);
        Collections.sort(allSymptomDetails, Collections.reverseOrder());
        log.debug("symptomDetails after sorting : {}", allSymptomDetails);
        return allSymptomDetails;
    }

    // Skip Details if:
    // 1) detail value is blank
    // 2) is ProCtcae survey && patient replies with 0 for compulsory questions for pain, nausea
    private boolean skipSymptomDetails(final String symptomType, final String value, boolean isProCtcaeFormat) {
        log.debug("symptomType : {}, value : '{}', proCtcae ? : {}", symptomType, value, isProCtcaeFormat);

        if (StringUtils.isBlank(value))
            return true;

        if (isProCtcaeFormat &&
            (((symptomType.equalsIgnoreCase("pain") || symptomType.equalsIgnoreCase("nausea")) &&
            value.equals("0")))) {
            return true;
        }

        return false;
    }
}
