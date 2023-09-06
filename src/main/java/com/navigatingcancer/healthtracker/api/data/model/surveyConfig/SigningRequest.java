package com.navigatingcancer.healthtracker.api.data.model.surveyConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.navigatingcancer.healthtracker.api.data.model.LanguageType;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SigningRequest {

    @JsonInclude(NON_NULL)
    private String id;
    private Long patientId;
    private Long clinicId;
    private Long locationId;
    private String templateId;
    private String status;
    private Date createdDate;
    private Date updatedDate;
    private LanguageType language;
    private String emailSubject;
    private String emailBlurb;
    private List<SigningRequestRole> roles = new ArrayList<>();
}
