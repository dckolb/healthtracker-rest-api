package com.navigatingcancer.healthtracker.api.processor.model;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;

import org.kie.api.runtime.KieContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DroolsManager {

	@Value(value = "${MN.clinicIds}")
    public Long[] MN_CLINIC_IDs;

    private Set<Long> mnIDs;

    @Autowired
    private ProFormatManager proFormatManager;

	@Autowired
	private KieContainer kieContainer;

    @Autowired
    private KieContainer kiePROCTCAEContainer;

    @Autowired
    private KieContainer kieMNContainer;

    @Autowired
    private KieContainer kieStatusCheck;

    @Autowired
    private KieContainer kieCTCAEStatusCheck;

    @PostConstruct
    public void init() {
        if( MN_CLINIC_IDs != null ) {
            mnIDs = Set.of(MN_CLINIC_IDs);
        } else {
            log.warn("MN clinic IDs not defined in the configuration");
            mnIDs = new HashSet<>();
        }
    }

    public KieContainer getRulesForClinicAndTreatmentType(final Enrollment enrollment) {
        log.info("getAgendasForClinicAndTreatmentType called for enrollment : {}", enrollment);
        if( mnIDs.contains(enrollment.getClinicId()) ) {
            log.debug("follow MN rules");
            return kieMNContainer;
        } else if(proFormatManager.followsCtcaeStandard(enrollment)) {
            log.debug("matches pro ctcae");
            return kiePROCTCAEContainer;
        }
        else {
            log.debug("does not match pro ctcae");
            return kieContainer;
        }
    }

    public KieContainer getRulesForStatusCheck(final Enrollment enrollment) {
        log.info("getAgendasForClinicAndTreatmentType called for enrollment : {}", enrollment);
        if (mnIDs.contains(enrollment.getClinicId()) || proFormatManager.followsCtcaeStandard(enrollment)) {
            log.debug("matches pro ctcae");
            return kieCTCAEStatusCheck;
        } else {
            log.debug("does not match pro ctcae");
            return kieStatusCheck;
        }

    }
}
