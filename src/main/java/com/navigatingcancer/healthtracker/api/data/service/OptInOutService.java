package com.navigatingcancer.healthtracker.api.data.service;

import java.util.List;

import javax.validation.Valid;

import com.navigatingcancer.healthtracker.api.data.model.OptInOut;

public interface OptInOutService {

    List<OptInOut> getOptInOutRecords(Long clinicId, Long locationId, Long patientId, String actionFIlter,
            String surveyId);

    OptInOut save(@Valid OptInOut body);

}