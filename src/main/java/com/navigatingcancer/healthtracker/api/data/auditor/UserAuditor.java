package com.navigatingcancer.healthtracker.api.data.auditor;

import com.navigatingcancer.healthtracker.api.data.auth.IdentityContext;
import com.navigatingcancer.healthtracker.api.data.auth.IdentityContextHolder;
import com.navigatingcancer.healthtracker.api.data.service.PatientInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class UserAuditor implements AuditorAware<String> {

    private IdentityContextHolder identityContextHolder;

    private PatientInfoService patientInfoService;

    @Autowired
    public UserAuditor(IdentityContextHolder identityContextHolder, PatientInfoService patientInfoService) {
        this.identityContextHolder = identityContextHolder;
        this.patientInfoService = patientInfoService;
    }


    @Override
    public Optional<String> getCurrentAuditor() {
        IdentityContext identityContext = this.identityContextHolder.get();

        log.debug("identityContext {}", identityContext);
        if (identityContext != null ){
            if (identityContext.getClinicianName() != null ) {
                return Optional.of(
                        identityContext.getClinicianName()
                );
            } else if (identityContext.getPatientId() != null && identityContext.getClinicId() != null ){
                return Optional.of(this.patientInfoService.getPatientName(identityContext.getClinicId(), identityContext.getPatientId()));
            }
        }
        log.debug("defaulting to HT");
        return Optional.of("Health Tracker");
    }
}
