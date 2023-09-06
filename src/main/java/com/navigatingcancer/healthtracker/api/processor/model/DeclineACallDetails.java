package com.navigatingcancer.healthtracker.api.processor.model;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.service.CheckInService;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class DeclineACallDetails {
    private boolean callBackDeclined;
    private String comments;

    public static DeclineACallDetails fromPayloads(List<SurveyItemPayload> payloads) {
        DeclineACallDetails details = new DeclineACallDetails();


        boolean callBackDeclined = false;
        String comment = null;

        for(SurveyItemPayload payload : payloads) {
            if(payload == null) {
                continue;
            }
            callBackDeclined = callBackDeclined || payload.isDeclineACall();
            if(comment == null){
                comment = payload.getDeclineACallComment();
            }
        }

        details.setCallBackDeclined(callBackDeclined);
        details.setComments(comment);

        return details;
    }
}
