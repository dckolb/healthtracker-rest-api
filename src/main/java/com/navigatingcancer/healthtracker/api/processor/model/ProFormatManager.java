package com.navigatingcancer.healthtracker.api.processor.model;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.TherapyType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class ProFormatManager {

    @Value("#{'${proCtcae.clinicIds}'.split(',')}")
    private final Set<Long> PRO_CTCAE_CLINICS = new HashSet<>();

    public boolean followsCtcaeStandard(Enrollment enrollment) {
        if(enrollment.getClinicId() == null || enrollment.getTherapyTypes() == null || enrollment.getTherapyTypes().isEmpty())
            return false;

        return PRO_CTCAE_CLINICS.contains(enrollment.getClinicId()) && enrollment.getTherapyTypes().contains(TherapyType.IV);
    }
}
