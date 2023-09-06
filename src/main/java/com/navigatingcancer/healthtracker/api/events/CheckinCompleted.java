package com.navigatingcancer.healthtracker.api.events;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;

import lombok.Data;

@Data
public class CheckinCompleted extends CheckinEvent {
    public enum Type {
        SYMPTOM, ORAL, SYMPTOM_ORAL
    }
    Type type;
    SurveyPayload surveyPayload;
}
