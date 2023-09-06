package com.navigatingcancer.healthtracker.api.processor.model;

import com.google.common.base.Strings;
import com.navigatingcancer.healthtracker.api.data.model.Adherence;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class AdherenceParser {
    private static final String MEDICATION_SKIP_REASON_KEY = "medicationSkipReason" ;
    private static final String MEDICATION_SKIP_REASON_DETAILS_KEY = "medicationSkipReasonComment" ;
    private static final String MEDICATION_TAKEN_KEY = "medicationTaken" ;
    private static final String MEDICATION_STARTED_KEY = "medicationStarted";
    private static final String MEDICATION_STARTED_DATE_KEY = "medicationStartedDate";
    private static final String SCHEDULE_DATE_KEY = "scheduleDate";
    
    public static final String MEDICATION_TAKEN_YES = "TAKEN";
    public static final String MEDICATION_TAKEN_NO = "NOT_TAKEN";
    public static final String MEDICATION_STARTED_YES = "STARTED";
    public static final String MEDICATION_STARTED_NO = "NOT_STARTED";

    public List<Adherence> parse(List<SurveyItemPayload> orals, final String medicationName) {
        List<Adherence> parsedAdherences = new ArrayList<>();
        if (orals != null) {
            orals.forEach(oral -> {
                Adherence adherence = new Adherence();
                Map<String, Object> payload = oral.getPayload();
                adherence.setMedication(medicationName);
                String status = (String) payload.get(MEDICATION_TAKEN_KEY);
                String started = (String) payload.get(MEDICATION_STARTED_KEY);
                String startDate = (String) payload.get(MEDICATION_STARTED_DATE_KEY);
                String scheduleDate = (String) payload.get(SCHEDULE_DATE_KEY);
                Boolean tookMedication = "yes".equalsIgnoreCase(status) || "yes".equalsIgnoreCase(started);
                Boolean startedMedication = !Strings.isNullOrEmpty(started);
                Boolean hasScheduleDate = !Strings.isNullOrEmpty(scheduleDate);
                if (tookMedication){
                    status = startedMedication ? MEDICATION_STARTED_YES : MEDICATION_TAKEN_YES;
                } else {
                    status = startedMedication ? MEDICATION_STARTED_NO : MEDICATION_TAKEN_NO;
                }

                if (startedMedication) {
                    adherence.setPatientReportedStartDate(startDate);
                    log.debug("started {}", started);
                }

                if(hasScheduleDate) {
                    adherence.setScheduleDate(scheduleDate);
                }

                adherence.setStatus(status);
                adherence.setReasonDetails((String)payload.getOrDefault(MEDICATION_SKIP_REASON_DETAILS_KEY, ""));

                String medsSkippedReason =
                        SurveyDictionary.medicationSkipDescription((String) payload.get(MEDICATION_SKIP_REASON_KEY),
                                "Other");
                
                adherence.setReason(medsSkippedReason);
                parsedAdherences.add(adherence);
            });
        }
        return parsedAdherences;
    }
}
