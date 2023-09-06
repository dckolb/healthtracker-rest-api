package com.navigatingcancer.healthtracker.api.data.service.impl;

import java.util.Arrays;
import java.util.List;

import com.navigatingcancer.healthtracker.api.data.auth.Identity;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.service.PatientRequestService;
import com.navigatingcancer.healthtracker.api.events.HealthTrackerEventsPublisher;
import com.navigatingcancer.healthtracker.api.processor.model.TriageRequestPayload;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import com.navigatingcancer.healthtracker.api.rest.exception.RabbitMQException;
import com.navigatingcancer.healthtracker.api.rest.representation.PatientRequest;
import com.navigatingcancer.json.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PatientRequestServiceImpl implements PatientRequestService {

    RabbitTemplate rabbitTemplate;

    public static String TRIAGE_QUEUE_NAME = "app/health_tracker/patient/create_incident";

    @Autowired
    HealthTrackerEventsPublisher eventsPublisher;

    @Autowired
    Identity identity;

    @Autowired
	private EnrollmentRepository enrollmentRepository;

    @Autowired
    public PatientRequestServiceImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void requestCall(PatientRequest callRequest) {
        log.debug("PatientRequestServiceImpl::requestCall");
        Enrollment enrollment = getEnrollment(callRequest.getPatientId(), callRequest.getClinicId());
        Long locationId = enrollment != null ? enrollment.getLocationId() : null;
        Long providerId = enrollment != null ? enrollment.getProviderId() : null;
        callRequest.setLocationId(locationId);
        callRequest.setProviderId(providerId);
        try {
            this.rabbitTemplate.convertAndSend(TRIAGE_QUEUE_NAME, JsonUtils.toJson(TriageRequestPayload.createCallPayload(callRequest)));
        } catch (AmqpException ex){
            log.error("unable to send request a call message to rabbit", ex);
            throw new RabbitMQException("unable to send request");
        }
        eventsPublisher.publishRequestCallEvent(callRequest.getPatientId(), callRequest.getClinicId(), callRequest.getPayload(), identity);
    }

    @Override
    public void requestRefill(PatientRequest refillRequest) {
        log.debug("PatientRequestServiceImpl::requestRefill");
        Enrollment enrollment = getEnrollment(refillRequest.getPatientId(), refillRequest.getClinicId());
        Long locationId = enrollment != null ? enrollment.getLocationId() : null;
        Long providerId = enrollment != null ? enrollment.getProviderId() : null;
        refillRequest.setLocationId(locationId);
        refillRequest.setProviderId(providerId);
        try {
            this.rabbitTemplate.convertAndSend(TRIAGE_QUEUE_NAME, JsonUtils.toJson(TriageRequestPayload.createRefillPayload(refillRequest)));
        } catch (AmqpException ex){
            log.error("unable to send request a refill message to rabbit", ex);
            throw new RabbitMQException("unable to send request");
        }
        eventsPublisher.publishRequestRefillEvent(refillRequest.getPatientId(), refillRequest.getClinicId(), refillRequest.getPayload(), identity);
    }

    private Enrollment getEnrollment(Long patientId, Long clinicId) {
        EnrollmentQuery query = new EnrollmentQuery();
        query.setPatientId(Arrays.asList(patientId));
        query.setClinicId(Arrays.asList(clinicId));
        query.setStatus(Arrays.asList(EnrollmentStatus.ACTIVE));
		query.setAll(true);
		List<Enrollment> enrollments = enrollmentRepository.findEnrollments(query);
        Enrollment enrollment = enrollments.size() > 0 ? enrollments.get(0) : null ;
       
        return enrollment;
    }
}
