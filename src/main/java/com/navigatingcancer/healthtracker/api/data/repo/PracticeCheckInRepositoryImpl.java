package com.navigatingcancer.healthtracker.api.data.repo;

import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.model.PracticeCheckIn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Query.query;

@Repository
public class PracticeCheckInRepositoryImpl implements PracticeCheckInRepository{
    @Autowired
    private MongoTemplate template;

    @Override
    public PracticeCheckIn save(PracticeCheckIn checkin) {
        return template.save(checkin);
    }

    @Override
    public Optional<PracticeCheckIn> findFirstByClinicIdAndPatientId(Long clinicId, Long patientId){
        Criteria criteria = new Criteria();
        criteria.and("clinicId").is(clinicId);
        criteria.and("patientId").is(patientId);
        return template.query(PracticeCheckIn.class)
                .matching(query(criteria).with(Sort.by(Sort.Direction.DESC, "date"))).first();
    }
}
