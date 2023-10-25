package com.navigatingcancer.healthtracker.api.data.repo;

import com.google.common.base.Preconditions;
import com.mongodb.client.result.UpdateResult;
import com.navigatingcancer.healthtracker.api.data.model.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class CustomCheckInRepositoryImpl implements CustomCheckInRepository {

  @Autowired MongoTemplate mongoTemplate;

  @Override
  public CheckIn upsertByNaturalKey(CheckIn ci) {
    Date now = new Date();

    Criteria criteria =
        Criteria.where("enrollmentId")
            .is(ci.getEnrollmentId())
            .and("scheduleDate")
            .is(ci.getScheduleDate())
            .and("status")
            .is(CheckInStatus.PENDING);

    // always match check-in type against incoming check-in
    var checkInTypeMatches = Criteria.where("checkInType").is(ci.getCheckInType());
    var surveyInstanceMatches = Criteria.where("surveyInstanceId").is(ci.getSurveyInstanceId());

    if (ci.getSurveyInstanceId() != null) {
      // if incoming has a check-in type use that in conjunction with the survey instance
      if (ci.getCheckInType() != null) {
        criteria =
            new Criteria()
                .andOperator(
                    criteria, new Criteria().orOperator(surveyInstanceMatches, checkInTypeMatches));
      } else {
        criteria = new Criteria().andOperator(criteria, surveyInstanceMatches);
      }
    } else {
      Preconditions.checkNotNull(
          ci.getCheckInType(), "check-in type MUST be set if survey instance id is not");
      criteria = new Criteria().andOperator(criteria, checkInTypeMatches);
    }

    Query q = new Query(criteria);
    Update update =
        new Update()
            .setOnInsert("status", ci.getStatus())
            .setOnInsert("patientId", ci.getPatientId())
            .setOnInsert("clinicId", ci.getClinicId())
            .setOnInsert("locationId", ci.getLocationId())
            .setOnInsert("scheduleDate", ci.getScheduleDate())
            .setOnInsert("scheduleTime", ci.getScheduleTime())
            .setOnInsert("checkInScheduleId", ci.getCheckInScheduleId())
            .setOnInsert("surveyInstanceId", ci.getSurveyInstanceId())
            .setOnInsert("checkInParameters", ci.getCheckInParameters())
            .setOnInsert("checkInType", ci.getCheckInType())
            .setOnInsert("surveyId", ci.getSurveyId())
            .setOnInsert("contactPreferences", ci.getContactPreferences())
            .setOnInsert("createdDate", now)
            .setOnInsert("updatedDate", now)
            .setOnInsert("createdBy", "Health Tracker")
            .setOnInsert("updatedBy", "Health Tracker")
            .setOnInsert("createdReason", ci.getCreatedReason());

    return this.mongoTemplate.findAndModify(
        q, update, new FindAndModifyOptions().returnNew(true).upsert(true), CheckIn.class);
  }

  // Generated query : [ { "$match" : { "enrollmentId" : "EID1" , "status" : "COMPLETED"}} , {
  // "$group" : { "_id" : "$scheduleDate"}}]
  @Override
  public Integer getCompletedCount(String enrollmentId) {
    // NOTE: Currently we have only 1 enrollment per scheduleDate
    // Once we add multiple (per med), we will need to group by medicine too
    return mongoTemplate
        .aggregateAndReturn(CheckIn.class)
        .by(
            Aggregation.newAggregation(
                Aggregation.match(
                    Criteria.where("enrollmentId")
                        .is(enrollmentId)
                        .and("status")
                        .is(CheckInStatus.COMPLETED)),
                Aggregation.group("scheduleDate")))
        .all()
        .getMappedResults()
        .size();
  }

  // Generated query : [ { "$match" : { "enrollmentId" : "EID2" , "status" : { "$ne" : "PENDING"}}}
  // , { "$group" : { "_id" : "$scheduleDate"}}]
  @Override
  public Integer getTotalCount(String enrollmentId) {
    // NOTE: Currently we have only 1 enrollment per scheduleDate
    // Once we add multiple (per med), we will need to group by medicine too
    return mongoTemplate
        .aggregateAndReturn(CheckIn.class)
        .by(
            Aggregation.newAggregation(
                Aggregation.match(
                    Criteria.where("enrollmentId")
                        .is(enrollmentId)
                        .and("status")
                        .ne(CheckInStatus.PENDING)),
                Aggregation.group("scheduleDate")))
        .all()
        .getMappedResults()
        .size();
  }

  @Override
  public float getAdherencePercent(String enrollmentId) {
    int totalOralCount = getTotalOralCount(enrollmentId);
    int totalOralMedsTakenCount = getTotalOralMedsTakenCount(enrollmentId);

    if (totalOralMedsTakenCount > totalOralCount) {
      // This should not really happen
      log.warn(
          "Error computing adherence for Enrollment {}. TotalOralCount = {}, TotalOralMedsTakenCount = {}",
          enrollmentId,
          totalOralCount,
          totalOralMedsTakenCount);
      return 100;
    }

    if (totalOralCount == 0) {
      return 0;
    } else {
      return ((float) totalOralMedsTakenCount / totalOralCount) * 100;
    }
  }

  // Generated query : [ { "$match" : { "enrollmentId" : "EID1" , "status" : { "$ne" : "PENDING"} ,
  // "checkInType" : "ORAL"}} , { "$group" : { "_id" : "$scheduleDate"}}]
  @Override
  public Integer getTotalOralCount(String enrollmentId) {
    // NOTE: Currently we have only 1 enrollment per scheduleDate
    // Once we add multiple (per med), we will need to group by medicine too
    return mongoTemplate
        .aggregateAndReturn(CheckIn.class)
        .by(
            Aggregation.newAggregation(
                Aggregation.match(
                    Criteria.where("enrollmentId")
                        .is(enrollmentId)
                        .and("status")
                        .ne(CheckInStatus.PENDING)
                        .and("checkInType")
                        .is(CheckInType.ORAL)),
                Aggregation.group("scheduleDate")))
        .all()
        .getMappedResults()
        .size();
  }

  // Generated Query : [ { "$match" : { "enrollmentId" : "EID1" , "status" : { "$ne" : "PENDING"} ,
  // "medicationTaken" : true , "checkInType" : "ORAL"}} , { "$group" : { "_id" : "$scheduleDate"}}]
  @Override
  public Integer getTotalOralMedsTakenCount(String enrollmentId) {
    // NOTE: Currently we have only 1 enrollment per scheduleDate
    // Once we add multiple (per med), we will need to group by medicine too

    return mongoTemplate
        .aggregateAndReturn(CheckIn.class)
        .by(
            Aggregation.newAggregation(
                Aggregation.match(
                    Criteria.where("enrollmentId")
                        .is(enrollmentId)
                        .and("status")
                        .ne(CheckInStatus.PENDING)
                        .and("medicationTaken")
                        .is(true)
                        .and("checkInType")
                        .is(CheckInType.ORAL)),
                Aggregation.group("scheduleDate")))
        .all()
        .getMappedResults()
        .size();
  }

  static final class EnrollmentSearchResult {
    public String enrollmentId;
    public String status;
  }
  ;

  @Override
  public List<String> findCheckIns(
      List<String> enrollmentIds, List<String> status, CheckInType checkInType) {
    // Sort by enrollment+scheduleDate, use descending order to get most recent checkins first
    SortOperation sort = Aggregation.sort(Direction.DESC, "enrollmentId", "scheduleDate");
    MatchOperation checkinsFilter =
        Aggregation.match(
            Criteria.where("enrollmentId").in(enrollmentIds).and("checkInType").is(checkInType));
    // Use group by enrollment ID to find last checkin status for enrollment
    GroupOperation lastEnrCheckinStatus =
        Aggregation.group("enrollmentId").first("status").as("status");

    Aggregation agg =
        Aggregation.newAggregation(
            sort,
            checkinsFilter,
            lastEnrCheckinStatus,
            Aggregation.project("status")
                .and("enrollmentId")
                .previousOperation() // Preserve the names
            );
    AggregationResults<EnrollmentSearchResult> aggRes =
        mongoTemplate.aggregate(agg, "checkins", EnrollmentSearchResult.class);

    // Narrowed list to enrollment (IDs) that have last checkin that is
    // pending or missed
    List<String> retval =
        aggRes.getMappedResults().stream()
            .filter(r -> status.contains(r.status))
            .map(r -> r.enrollmentId)
            .collect(Collectors.toList());
    // Convert strings to ObjectIds
    List<ObjectId> eids = retval.stream().map(i -> new ObjectId(i)).collect(Collectors.toList());

    // Enrollments from the list that are ACTIVE.
    MatchOperation enrFilter =
        Aggregation.match(Criteria.where("_id").in(eids).and("status").is("ACTIVE"));
    // select id only
    ProjectionOperation selectEid = Aggregation.project("_id");
    // aggregate
    agg = Aggregation.newAggregation(enrFilter, selectEid);
    AggregationResults<Enrollment> enrollments =
        mongoTemplate.aggregate(agg, "enrollments", Enrollment.class);

    return enrollments.getMappedResults().stream().map(e -> e.getId()).collect(Collectors.toList());
  }

  @Override
  public boolean isPending(String... enrollmentIds) {
    return mongoTemplate
            .aggregateAndReturn(CheckIn.class)
            .by(
                Aggregation.newAggregation(
                    Aggregation.match(
                        Criteria.where("enrollmentId")
                            .in(enrollmentIds)
                            .and("status")
                            .is(CheckInStatus.PENDING)),
                    Aggregation.group("enrollmentId")))
            .all()
            .getMappedResults()
            .size()
        > 0;
  }

  @Override
  public Integer getMissedCheckins(String... enrollmentIds) {
    return mongoTemplate
        .aggregateAndReturn(CheckIn.class)
        .by(
            Aggregation.newAggregation(
                Aggregation.match(
                    Criteria.where("enrollmentId")
                        .in(enrollmentIds)
                        .and("status")
                        .is(CheckInStatus.MISSED)),
                Aggregation.group("scheduleDate")))
        .all()
        .getMappedResults()
        .size();
  }

  static Query getMissedCheckinsQuery(String enrollmentId, LocalDate fromDate) {
    // @Query(value = "{enrollmentId : ?0, status: 'PENDING', scheduleDate: {$lte:?1}}")
    Query q =
        Query.query(
            Criteria.where("enrollmentId")
                .is(enrollmentId)
                .and("scheduleDate")
                .lte(fromDate)
                .and("status")
                .is(CheckInStatus.PENDING));
    return q;
  }

  @Override
  public List<CheckIn> getMissedCheckins(String enrollmentId, LocalDate fromDate) {
    return mongoTemplate.find(getMissedCheckinsQuery(enrollmentId, fromDate), CheckIn.class);
  }

  @Override
  public Long setMissedCheckins(String enrollmentId, LocalDate fromDate) {
    Query q = getMissedCheckinsQuery(enrollmentId, fromDate);
    Update u = new Update();
    u.set("status", CheckInStatus.MISSED);
    u = AbstractDocument.appendUpdatedBy(u);
    // NOTE. According to AWS documentation updateMulti in docdb is atomic and consistent
    UpdateResult res = mongoTemplate.updateMulti(q, u, CheckIn.class);
    return res.getModifiedCount();
  }

  // count how many checkins missed since the last completed checkin
  @Override
  public Long getLastMissedCheckinsCount(String enrollmentId) {
    Query q =
        Query.query(Criteria.where("enrollmentId").is(enrollmentId))
            .with(Sort.by(Sort.Order.desc("scheduleDate")));
    CheckIn[] allCheckins = mongoTemplate.find(q, CheckIn.class).toArray(new CheckIn[0]);
    long res = 0;
    int pos = 0;
    // skip all pending checkins
    while (pos < allCheckins.length && allCheckins[pos].getStatus() == CheckInStatus.PENDING) pos++;
    // count missed checkin days before any other non-missed status (must be completed)
    if (pos < allCheckins.length && allCheckins[pos].getStatus() == CheckInStatus.MISSED) {
      res = 1;
      LocalDate countedDate = allCheckins[pos++].getScheduleDate();
      while (pos < allCheckins.length && allCheckins[pos].getStatus() == CheckInStatus.MISSED) {
        if (!allCheckins[pos].getScheduleDate().equals(countedDate)) {
          res++;
          countedDate = allCheckins[pos].getScheduleDate();
        }
        pos++;
      }
      ;
    }
    return res;
  }

  @Override
  public Long stopCheckins(String enrollmentId) {
    Query q =
        Query.query(
            Criteria.where("enrollmentId")
                .is(enrollmentId)
                .and("status")
                .is(CheckInStatus.PENDING));
    Update u = new Update();
    u.set("status", CheckInStatus.STOPPED);
    u = AbstractDocument.appendUpdatedBy(u);
    UpdateResult res = mongoTemplate.updateMulti(q, u, CheckIn.class);
    return res.getModifiedCount();
  }

  // Given list of checkin IDs find the last one of a specific type
  @Override
  public CheckIn getLastCheckinByType(List<String> checkInIds, CheckInType type) {
    Criteria search = Criteria.where("_id").in(checkInIds).and("checkInType").is(type);
    Query q = Query.query(search).with(Sort.by(Sort.Order.desc("scheduleDate"))).limit(1);
    CheckIn res = mongoTemplate.findOne(q, CheckIn.class);
    return res;
  }

  @Override
  public boolean hasCompletedACheckIn(Long patiendId) {
    Criteria search =
        Criteria.where("patientId").in(patiendId).and("status").is(CheckInStatus.COMPLETED);
    Query q = Query.query(search);
    CheckIn res = mongoTemplate.findOne(q, CheckIn.class);
    return res != null;
  }

  @Override
  public void updateFieldsById(String checkInId, Map<String, Object> updates) {
    Preconditions.checkNotNull(checkInId);
    Preconditions.checkNotNull(updates);
    var query = Query.query(Criteria.where("_id").is(new ObjectId(checkInId)));
    var update = mapToUpdate(updates);
    mongoTemplate.updateFirst(query, update, CheckIn.class);
  }

  @Override
  public List<CheckIn> findCheckInsBySchedule(String enrollmentId, CheckInSchedule schedule) {

    List<Criteria> scheduleCriteria = new ArrayList<>();
    if (schedule.getId() != null) {
      scheduleCriteria.add(Criteria.where("checkInScheduleId").is(schedule.getId()));
    }

    if (schedule.getCheckInType() != null) {
      scheduleCriteria.add(Criteria.where("checkInType").is(schedule.getCheckInType()));
    }

    if (scheduleCriteria.isEmpty()) {
      throw new IllegalArgumentException(
          "unable to lookup check-ins for schedule missing id and check-in type");
    }

    var query =
        Query.query(
            new Criteria()
                .andOperator(
                    Criteria.where("enrollmentId").is(enrollmentId),
                    new Criteria().orOperator(scheduleCriteria)));
    query.with(Sort.by("scheduleDate"));
    return mongoTemplate.find(query, CheckIn.class);
  }

  @Override
  public long bulkUpdateFieldsByCheckInType(
      String enrollmentId,
      CheckInType checkInType,
      CheckInStatus checkInStatus,
      Map<String, Object> updates) {
    Preconditions.checkNotNull(enrollmentId);
    Preconditions.checkNotNull(checkInType);

    var query =
        Query.query(
            Criteria.where("enrollmentId")
                .is(enrollmentId)
                .and("checkInType")
                .is(checkInType)
                .and("status")
                .is(checkInStatus));

    var update = mapToUpdate(updates);

    var res = mongoTemplate.updateMulti(query, update, CheckIn.class);

    return res.getModifiedCount();
  }

  private static Update mapToUpdate(Map<String, Object> updates) {
    var update = new Update();
    updates.forEach(update::set);
    return update;
  }
}
