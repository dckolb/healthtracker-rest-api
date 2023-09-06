package com.navigatingcancer.healthtracker.api.data.repo;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatusLog;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Concrete implementation of findEnrollments
 *
 * This class is instantiated by Spring Data autoconfiguration since the EnrollmentRepository extends
 * the CustomEnrollmentRepository and this implementation of that interface is suffixed with 'Impl'
 */
public class CustomEnrollmentRepositoryImpl implements CustomEnrollmentRepository {

    private static final Logger logger = LoggerFactory.getLogger(CustomEnrollmentRepository.class);
    private MongoTemplate template;

    @Autowired
    public CustomEnrollmentRepositoryImpl(MongoTemplate template){
        this.template = template;
    }

    @Override
    public Boolean activeEnrollmentExists(long patientId, long clinicId){
        Query query = new Query();
        Criteria criteria = Criteria.where("clinicId").is(clinicId).and("patientId").is(patientId)
                .and("status").in(Arrays.asList(EnrollmentStatus.ACTIVE,EnrollmentStatus.PAUSED ));
        query.addCriteria(criteria).limit(1); // only need one

        List<Enrollment> results = template.find(query, Enrollment.class);
        return results.size() == 1;
    }

    @Override
    public List<Enrollment> findEnrollmentsByIds(Long clinicId, List<String> ids){
        Query query = new Query();
        Criteria criteria = Criteria.where("_id").in(ids).and("clinicId").is(clinicId);
        query.addCriteria(criteria);
        return template.find(query, Enrollment.class);
    }

    @Override
    public List<Enrollment> findEnrollments(EnrollmentQuery params) {

        Query query = new Query();

        Criteria criteria = null;
        // NOTE: we don't have a 'default' filter at the moment so we need to
        // check that we have a criteria created
        if (params.getClinicId() != null && params.getClinicId().size() > 0){
            criteria = Criteria.where("clinicId")
                .in(params.getClinicId());
        }
        if (params.getId() != null && params.getId().size() > 0 ){
            if (criteria == null)
                criteria = Criteria.where("id")
                        .in(params.getId());
            else
                criteria.and("id").in(params.getId());
        }
        if (params.getLocationId() != null && params.getLocationId().size() > 0){
            if (criteria == null)
                criteria = Criteria.where("locationId")
                    .in(params.getLocationId());
            else
                criteria.and("locationId").in(params.getLocationId());

        }
        if (params.getPatientId() != null && params.getPatientId().size() > 0){
            if (criteria == null)
                criteria = Criteria.where("patientId")
                        .in(params.getPatientId());
            else
                criteria.and("patientId").in(params.getPatientId());
        }
        if (params.getStatus() != null && params.getStatus().size() > 0){
            if (criteria == null)
                criteria = Criteria.where("status")
                        .in(params.getStatus());
            else
                criteria.and("status").in(params.getStatus());
        }
        if (params.getEndDate() != null && params.getStartDate() != null){
            if (criteria == null)
                criteria = Criteria.where("txStartDate").gte(params.getStartDate())
                    .lte(params.getEndDate());
        }

        if (criteria == null){
            logger.error("no known parameters sent in findEnrollments query");
            throw new DataAccessException("no known parameters sent"){};
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");


        query.addCriteria(criteria);
        query.with(sort);
        logger.debug("query is {}", query);

        final Pageable pageableRequest = PageRequest.of(0,1);
        if (!params.isAll()) {
            query.with(pageableRequest);
        }
        return template.find(query,
                Enrollment.class);

    }

    @Override
    public List<Enrollment> getCurrentEnrollments(List<Long> clinicIds, List<Long> locationIds, List<Long> providerIds, Boolean isManualCollect) {
        Query query = new Query();
        Criteria criteria = Criteria.where("clinicId").in(clinicIds);
        query.addCriteria(criteria);
        query.fields().include("_id").include("patientId");

        List<Criteria> commonAndCriterion = getAndCriteriaForCurrentEnrollments(locationIds, providerIds, isManualCollect);

        if( isManualCollect != null && isManualCollect ) {
            addCriterionForManualCollect(criteria, commonAndCriterion);
        } else {
            criteria  = criteria.orOperator(getOrCriteriaForCurrentEnrollments());
            if (commonAndCriterion.size() != 0) {
                Criteria[] andCriteriaArray = new Criteria[commonAndCriterion.size()];
                commonAndCriterion.toArray(andCriteriaArray);
                criteria.andOperator(andCriteriaArray);
            }
        }

        logger.info("Query used for filtering is {}", query);

        return template.find(query, Enrollment.class);
    }

    private void addCriterionForManualCollect(Criteria criteria, List<Criteria> commonAndCriterion) {
        // Manual collect flag is used to get the Due items. These must be for active enrollments only
        criteria.and("status").is("ACTIVE");
        Criteria c1 = new Criteria();
        Criteria c1And = c1.andOperator(
                Criteria.where("reminderStartDate").exists(false), // if reminderStartDate exists then use that one
                Criteria.where("txStartDate").exists(true),
                Criteria.where("txStartDate").lte(LocalDate.now())
        );

        Criteria c2 = new Criteria();
        Criteria c2And = c2.andOperator(
                Criteria.where("reminderStartDate").exists(true),
                Criteria.where("reminderStartDate").lte(LocalDate.now())
        );

        Criteria c3 = new Criteria();
        Criteria c3Or = c3.orOperator(
                c2And, c1And
        );

        commonAndCriterion.add(c3Or);
        Criteria[] andCriteriaArray = new Criteria[commonAndCriterion.size()];
        commonAndCriterion.toArray(andCriteriaArray);
        criteria.andOperator(andCriteriaArray);



    }

    private Criteria[] getOrCriteriaForCurrentEnrollments() {
        List<Criteria> orCriterion = new ArrayList<>();

        orCriterion.add(Criteria.where("status").nin(EnrollmentStatus.COMPLETED, EnrollmentStatus.STOPPED));
        orCriterion.add(Criteria.where("updatedDate").gte(LocalDate.now().minusDays(14)));

        Criteria[] orCriteriaArray = new Criteria[orCriterion.size()];
        return orCriterion.toArray(orCriteriaArray);
    }

    private List<Criteria> getAndCriteriaForCurrentEnrollments(List<Long> locationIds, List<Long> providerIds, Boolean isManualCollect) {
    // HT-2118 locationId and providerid no longer always a clean and,
        List<Criteria> andCriterion = new ArrayList<>();

        if (locationIds != null && !locationIds.isEmpty()) {
            // handle zero as special case
            if (locationIds.contains(0L)){
                Criteria c = new Criteria();
                c.orOperator(Criteria.where("locationId").in(locationIds),
                        Criteria.where("locationId").exists(false));
                andCriterion.add(c);
            } else {
                andCriterion.add(Criteria.where("locationId").in(locationIds));
            }
        }
        if (providerIds != null && !providerIds.isEmpty()) {
            // handle zero as special case
            if (providerIds.contains(0L)){
                Criteria c = new Criteria();
                c.orOperator(Criteria.where("providerId").in(providerIds),
                        Criteria.where("providerId").exists(false));
                andCriterion.add(c);
            } else {
                andCriterion.add(Criteria.where("providerId").in(providerIds));
            }
        }
        if (isManualCollect != null) {
            andCriterion.add(Criteria.where("manualCollect").is(isManualCollect));
        }

        return andCriterion;
    }

	public Enrollment setStatus(String id, EnrollmentStatus status) {
		Query query = new Query(Criteria.where("_id").is(id));
		Update update = new Update().set("status", status);
		return template.findAndModify(query, update, new FindAndModifyOptions().returnNew(true), Enrollment.class);
    }

    public Enrollment setConsentRequestId(String id, String consentRequestId){
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().set("consentRequestId", consentRequestId);
        return template.findAndModify(query, update, new FindAndModifyOptions().returnNew(true), Enrollment.class);
    }

    @Override
    public Enrollment updateConsentStatus(String consentRequestId, String consentStatus, Date updatedDate) {
        Query query = new Query(Criteria.where("consentRequestId").is(consentRequestId));
        Update update = new Update().set("consentStatus", consentStatus)
                .set("consentUpdatedDate", updatedDate);
        return template.findAndModify(query, update, new FindAndModifyOptions().returnNew(true), Enrollment.class);
    }

    public Enrollment setUrl(String id, String url) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().set("url", url);
        return template.findAndModify(query, update, new FindAndModifyOptions().returnNew(true), Enrollment.class);
    }

    @Override
    public Enrollment appendStatusLog(String id, EnrollmentStatusLog log) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().push("statusLogs").each(log);
        return template.findAndModify(query, update, new FindAndModifyOptions().returnNew(true), Enrollment.class);
    }
   
}
