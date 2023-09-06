package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.navigatingcancer.healthtracker.api.data.service.PatientInfoService;
import com.navigatingcancer.patientinfo.PatientInfoClient;
import com.navigatingcancer.patientinfo.domain.PatientInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PatientInfoServiceImpl implements PatientInfoService {

    private final PatientInfoClient patientInfoClient;

    @Autowired
    public PatientInfoServiceImpl(PatientInfoClient client){
        this.patientInfoClient = client;
    }

    @Override
    public String getPatientName(long clinicId, long patientId) {
        String result = null;
        for (PatientInfo pt : this.patientInfoClient.getApi().getPatients(clinicId, patientId)){
            result = String.format("%s %s", pt.getFirstName(), pt.getLastName());
            log.trace("returning {} as name", result);
            break;
        }
        return result;
    }

}
