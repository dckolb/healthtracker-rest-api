package com.navigatingcancer.healthtracker.api.data.repo;

import static org.springframework.data.mongodb.core.query.Query.query;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.navigatingcancer.healthtracker.api.data.model.AbstractDocument;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class HealthTrackerEventsRepositoryImpl implements HealthTrackerEventsRepository {

    @Autowired
    private MongoTemplate template;

    @Override
    public HealthTrackerEvent save(HealthTrackerEvent e) {
        return template.save(e);
    }

    static final List<HealthTrackerEvent> deDupEvents(List<HealthTrackerEvent> e) {
        // Hash codes of the events in the list
        Set<Integer> seenHashes = new HashSet<>();
        // Test function to see if an event was seen already
        Predicate<HealthTrackerEvent> distinctTest = thisEvent -> {
            // Only reminder events de-duping is needed as of Jan 2021
            if (thisEvent.getType() != HealthTrackerEvent.Type.REMINDER_SENT) {
                return true;
            }
            // Calculate hash of only the most essential fiels: what, when and where
            Object[] essentialFields = new Object[] {
                thisEvent.getType(),
                thisEvent.getDate(),
                thisEvent.getEnrollmentId(),
                thisEvent.getRelatedCheckinId()
            };
            Integer thisHash = Arrays.deepHashCode(essentialFields);
            boolean seenHash = seenHashes.contains(thisHash);
            if (!seenHash) {
                seenHashes.add(thisHash);
            }
            return !seenHash;
        };
        // Filter events dropping
        return e.stream().filter(distinctTest).collect(Collectors.toList());
    }

    @Override
    public List<HealthTrackerEvent> getPatientEvents(Long clinicId, Long patientId) {
        Criteria criteria = new Criteria();
        criteria.and("clinicId").is(clinicId);
        criteria.and("patientId").is(patientId);
        var e = template.query(HealthTrackerEvent.class)
                .matching(query(criteria).with(Sort.by(Sort.Direction.DESC, "date"))).all();
        return deDupEvents(e);
    }

    @Override
    public List<HealthTrackerEvent> getEnrollmentEvents(String enrollmentId) {
        Criteria criteria = new Criteria();
        criteria.and("enrollmentId").is(enrollmentId);
        var e = template.query(HealthTrackerEvent.class)
                .matching(query(criteria).with(Sort.by(Sort.Direction.DESC, "date"))).all();
        return deDupEvents(e);
    }

    @Override
	public HealthTrackerEvent upsertCheckinEvent(HealthTrackerEvent e) {
		Query q = new Query(Criteria.where("enrollmentId").is(e.getEnrollmentId())
                .and("type").is(e.getType())
                .and("relatedCheckinId").is(e.getRelatedCheckinId())
                .and("date").is(e.getDate()));
		Update update = new Update().setOnInsert("date", e.getDate())
                .setOnInsert("event", e.getEvent())
                .setOnInsert("patientId", e.getPatientId())
				.setOnInsert("clinicId", e.getClinicId())
                .setOnInsert("by", e.getBy());
        if( e.getReason() != null ) {
            update.setOnInsert("reason", e.getReason());
        }
        if( e.getRelatedCheckins() != null ) {
            update.setOnInsert("relatedCheckins", e.getRelatedCheckins());
        }
        if( e.getProgramType() != null ) {
            update.setOnInsert("programType", e.getProgramType());
        }
        if( e.getSurveyType() != null ) {
            update.setOnInsert("surveyType", e.getSurveyType());
        }
        if( e.getNote() != null ) {
            update.setOnInsert("note", e.getNote());
        }
        if( e.getSideEffects() != null ) {
            update.setOnInsert("sideEffects", e.getSideEffects());
        }
        if( e.getOralAdherence() != null ) {
            update.setOnInsert("oralAdherence", e.getOralAdherence());
        }
        if( e.getMissedCheckinsCount() != null ) {
            update.setOnInsert("missedCheckinsCount", e.getMissedCheckinsCount());
        }
        update = AbstractDocument.appendUpdatedBy(update);
		return this.template.findAndModify(q, update, new FindAndModifyOptions().returnNew(true).upsert(true), HealthTrackerEvent.class);
	}


}