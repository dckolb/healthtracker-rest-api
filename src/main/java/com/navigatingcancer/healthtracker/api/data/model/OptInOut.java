package com.navigatingcancer.healthtracker.api.data.model;

import java.util.Date;

import com.navigatingcancer.healthtracker.api.rest.exception.BadDataException;
import com.navigatingcancer.healthtracker.api.rest.exception.MissingParametersException;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Document(collection = "optinout")
public class OptInOut extends AbstractDocument {
    public static enum Action {
        OPTED_IN, OPTED_OUT
    };

    String surveyId;
    Long clinicId;
    Long locationId;
    Long patientId;
    Action action;
    String reason;
    String note;
    @CreatedDate
    Date actionTimestamp;

    public static void validateInputValues(Long clinicIdValue, String actionValue)
            throws MissingParametersException, BadDataException {
        if (clinicIdValue == null) {
            throw new MissingParametersException("clinicId must be specified");
        }

        if (actionValue != null) {
            try {
                OptInOut.Action.valueOf(actionValue.toString());
            } catch (Exception ex) {
                throw new BadDataException("invalid action value filter: " + actionValue.toString());
            }
        }
    }
}