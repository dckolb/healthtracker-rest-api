package com.navigatingcancer.healthtracker.api.data.repo;

import com.google.common.base.Preconditions;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
public class CustomHealthTrackerStatusRepositoryImpl
    implements CustomHealthTrackerStatusRepository {

  private MongoTemplate template;

  @Autowired
  public CustomHealthTrackerStatusRepositoryImpl(MongoTemplate template) {
    this.template = template;
  }

  public HealthTrackerStatus save(HealthTrackerStatus status) {
    return template.save(status);
  }

  public List<HealthTrackerStatus> findByIds(Long clinicId, List<String> ids) {
    log.debug("HealthTrackerStatusRepository::findByIds");
    Query query = new Query();
    Criteria criteria = Criteria.where("_id").in(ids).and("clinicId").is(clinicId);
    query.addCriteria(criteria);
    log.debug(query.toString());
    return template.find(query, HealthTrackerStatus.class);
  }

  public List<HealthTrackerStatus> findStatuses(
      List<Long> clinicIds, List<Long> locationIds, List<Long> patientIds) {

    Query query = new Query();

    List<Criteria> criterion = new ArrayList<>();

    criterion.add(Criteria.where("status").ne(HealthTrackerStatusCategory.COMPLETED));
    criterion.add(Criteria.where("completedAt").gte(LocalDate.now().minusDays(14)));

    Criteria[] criteriaArray = new Criteria[criterion.size()];
    criteriaArray = criterion.toArray(criteriaArray);

    Criteria criteria = Criteria.where("clinicId").in(clinicIds).orOperator(criteriaArray);

    if (patientIds != null && !patientIds.isEmpty()) {
      criteria.and("patientInfo.id").in(patientIds);
    }

    query.addCriteria(criteria);
    return template.find(query, HealthTrackerStatus.class);
  }

  // this is here to avoid changing quite a bit of code to findById, which returns
  // Optional<HealthTrackerStatus>
  public HealthTrackerStatus getById(String id) {
    return template.findById(id, HealthTrackerStatus.class);
  }

  private static final FindAndModifyOptions returnNewOption =
      new FindAndModifyOptions().returnNew(true);

  public HealthTrackerStatus updateNextScheduleDate(String id, LocalDateTime date) {
    Query query = new Query(Criteria.where("_id").is(id));
    Update update = new Update().set("nextScheduleDate", date);
    return template.findAndModify(query, update, returnNewOption, HealthTrackerStatus.class);
  }

  public HealthTrackerStatus setEndCurrentCycle(String id, boolean state) {
    Query query = new Query(Criteria.where("_id").is(id));
    Update update = new Update().set("endCurrentCycle", state);
    return template.findAndModify(query, update, returnNewOption, HealthTrackerStatus.class);
  }

  public HealthTrackerStatus updateMissedCheckinDate(String id, Instant date) {
    Query query = new Query(Criteria.where("_id").is(id));
    Update update = new Update().set("lastMissedCheckInAt", date);
    return template.findAndModify(query, update, returnNewOption, HealthTrackerStatus.class);
  }

  @Override
  public HealthTrackerStatus updateCategory(String id, HealthTrackerStatusCategory cat) {
    Query query = new Query(Criteria.where("_id").is(id));
    Update update = new Update().set("category", cat);
    return template.findAndModify(query, update, returnNewOption, HealthTrackerStatus.class);
  }

  public HealthTrackerStatus findAndReplaceStatus(HealthTrackerStatus status) {
    Preconditions.checkArgument(status != null, "status can not be null");

    Query query = new Query(Criteria.where("_id").is(status.getId()));

    // Id must not be set for find and replace
    // https://docs.spring.io/spring-data/mongodb/docs/current/api/org/springframework/data/mongodb/core/MongoTemplate.html#findAndReplace(org.springframework.data.mongodb.core.query.Query,S,org.springframework.data.mongodb.core.FindAndReplaceOptions,java.lang.Class,java.lang.String,java.lang.Class)
    status.setId(null);
    FindAndReplaceOptions options = new FindAndReplaceOptions();
    options.returnNew();
    return template.findAndReplace(query, status, options);
  }
}
