package com.navigatingcancer.healthtracker.api.data.repo;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatusLog;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;

import java.util.Date;
import java.util.List;

public interface CustomEnrollmentRepository {
    List<Enrollment> findEnrollmentsByIds(Long clinicId, List<String> ids);
	List<Enrollment> findEnrollments(EnrollmentQuery params);
    Boolean activeEnrollmentExists(long patientId, long clinicId);
    List<Enrollment> getCurrentEnrollments(List<Long> clinicIds, List<Long> locationIds, List<Long> providerIds, Boolean isManualCollect);
	Enrollment setStatus(String id, EnrollmentStatus status);
    Enrollment setConsentRequestId(String id, String consentRequestId);
    Enrollment updateConsentStatus(String consentRequestId, String consentStatus, Date updatedDate);
    Enrollment setUrl(String id, String url);
    Enrollment appendStatusLog(String id, EnrollmentStatusLog log);
}
