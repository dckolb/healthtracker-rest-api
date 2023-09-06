package com.navigatingcancer.healthtracker.api.data.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import com.navigatingcancer.healthtracker.api.data.model.OptInOut;
import com.navigatingcancer.healthtracker.api.data.repo.CustomOptInOutRepository;
import com.navigatingcancer.healthtracker.api.data.service.OptInOutService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OptInOutServiceImpl implements OptInOutService {

    @Autowired
    private CustomOptInOutRepository customOptInOutRepository;

    @Override
    public List<OptInOut> getOptInOutRecords(Long clinicId, Long locationId, Long patientId, String actionFIlter,
            String surveyId) {

        OptInOut.validateInputValues(clinicId, actionFIlter);

        Map<String, Object> query = new HashMap<>();
        query.put("clinicId", clinicId);

        if (locationId != null) {
            query.put("locationId", locationId);
        }
        if (patientId != null) {
            query.put("patientId", patientId);
        }
        if (actionFIlter != null) {
            OptInOut.Action actionValue = OptInOut.Action.valueOf(actionFIlter.toString());
            query.put("action", actionValue);
        }
        if (surveyId != null) {
            query.put("surveyId", surveyId);
        }
        return customOptInOutRepository.getOptInOutRecords(query);
    }

    @Override
    public OptInOut save(@Valid OptInOut body) {
        OptInOut.validateInputValues(body.getClinicId(), body.getAction() != null ? body.getAction().name(): null);
        return customOptInOutRepository.save(body);
    }

}