package com.navigatingcancer.healthtracker.api.data.repo;

import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CustomHealthTrackerStatusRepositoryImpl implements CustomHealthTrackerStatusRepository {


	private MongoTemplate mongoTemplate;

	@Autowired
	public CustomHealthTrackerStatusRepositoryImpl(MongoTemplate mongoTemplate){
		this.mongoTemplate = mongoTemplate;
	}

	public HealthTrackerStatus save(HealthTrackerStatus status) {
		return mongoTemplate.save(status);
	}

	public  List<HealthTrackerStatus> findByIds(Long clinicId, List<String> ids){
		log.debug("HealthTrackerStatusRepository::findByIds");
		Query query = new Query();
		Criteria criteria = Criteria.where("_id").in(ids)
				.and("clinicId").is(clinicId);
		query.addCriteria(criteria);
		log.debug(query.toString());
		return mongoTemplate.find(query, HealthTrackerStatus.class);
	}

	public List<HealthTrackerStatus> findStatuses(List<Long> clinicIds, List<Long> locationIds, List<Long> patientIds) {

		Query query = new Query();

		List<Criteria> criterion = new ArrayList<>();

		criterion.add(Criteria.where("status").ne(HealthTrackerStatusCategory.COMPLETED));
		criterion.add(Criteria.where("completedAt").gte(LocalDate.now().minusDays(14)));

		Criteria[] criteriaArray = new Criteria[criterion.size()];
		criteriaArray = criterion.toArray(criteriaArray);

		Criteria criteria = Criteria.where("clinicId").in(clinicIds).orOperator(criteriaArray);

		if(patientIds != null && !patientIds.isEmpty()) {
			criteria.and("patientInfo.id").in(patientIds);
		}

		query.addCriteria(criteria);
		return mongoTemplate.find(query, HealthTrackerStatus.class);
	}

	// this is here to avoid changing quite a bit of code to findById, which returns Optional<HealthTrackerStatus>
	public HealthTrackerStatus getById(String id) {
		return mongoTemplate.findById(id, HealthTrackerStatus.class);
	}

	private static final FindAndModifyOptions returnNewOption = new FindAndModifyOptions().returnNew(true);

    public HealthTrackerStatus updateNextScheduleDate(String id, LocalDateTime date) {
		Query query = new Query(Criteria.where("_id").is(id));
		Update update = new Update().set("nextScheduleDate", date);
		return mongoTemplate.findAndModify(query, update, returnNewOption, HealthTrackerStatus.class);
	}

	public HealthTrackerStatus setEndCurrentCycle(String id, boolean state) {
		Query query = new Query(Criteria.where("_id").is(id));
		Update update = new Update().set("endCurrentCycle", state);
		return mongoTemplate.findAndModify(query, update, returnNewOption, HealthTrackerStatus.class);
	}

    public HealthTrackerStatus updateMissedCheckinDate(String id, Instant date) {
		Query query = new Query(Criteria.where("_id").is(id));
		Update update = new Update().set("lastMissedCheckInAt", date);
		return mongoTemplate.findAndModify(query, update, returnNewOption, HealthTrackerStatus.class);
	}

	@Override
	public HealthTrackerStatus updateCategory(String id, HealthTrackerStatusCategory cat) {
		Query query = new Query(Criteria.where("_id").is(id));
		Update update = new Update().set("category", cat);
		return mongoTemplate.findAndModify(query, update, returnNewOption, HealthTrackerStatus.class);
	}

}
