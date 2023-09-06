package com.navigatingcancer.healthtracker.api.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;

@RestController
public class CleanUpController {

	@Autowired
	EnrollmentRepository enrollmentRepository;

	@Autowired
	CheckInRepository checkInRepository;

	@RequestMapping(value = "/cleanup", method = RequestMethod.POST)
	public void cleanUp(@RequestParam(required = false) List<Long> locationId,
			@RequestParam(required = false) List<Long> clinicId, @RequestParam(required = false) List<Long> patientId) {

		EnrollmentQuery q = new EnrollmentQuery();
		q.setPatientId(patientId);
		q.setClinicId(clinicId);
		q.setLocationId(locationId);

		enrollmentRepository.findEnrollments(q).forEach(en -> {
			
			String enrollmentId = en.getId();
			
			checkInRepository.deleteByEnrollmentId(enrollmentId);
			enrollmentRepository.deleteById(enrollmentId);
		});

	}

}
