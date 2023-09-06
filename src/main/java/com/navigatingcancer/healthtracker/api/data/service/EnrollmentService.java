package com.navigatingcancer.healthtracker.api.data.service;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import com.navigatingcancer.healthtracker.api.rest.representation.EnrollmentIdentifiers;

import java.util.List;

public interface EnrollmentService {

    Enrollment createEnrollment(Enrollment enrollment);
    Enrollment updateEnrollment(Enrollment enrollment);
    Enrollment changeStatus(String id, EnrollmentStatus status, String reason, String note) throws Exception;
    void resendConsentRequest(String id) throws Exception;

    Enrollment getEnrollment(String id);
    List<EnrollmentIdentifiers> getCurrentEnrollments(List<Long> clinicIds, List<Long> locationIds, List<Long> providerIds, Boolean isManualCollect);
    List<Enrollment> getEnrollments(EnrollmentQuery params);

    List<Enrollment> getEnrollmentsByIds(List<String> enrollmentIds);

    Enrollment completeEnrollment(Enrollment enrollment);
    Enrollment appendEventsLog(String id, EnrollmentStatus status, String reason, String note, String clinicianId, String clinicianName);
    List<Enrollment> setProgramIds(List<Enrollment> enrollments);
	void rebuildSchedule(String id);
}
