package com.navigatingcancer.healthtracker.api.data.repo;

import java.util.List;
import java.util.stream.Collectors;

import com.navigatingcancer.healthtracker.api.data.model.CallAttempt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class CustomCallAttemptRepositoryImpl implements CustomCallAttemptRepository {
    private static final Logger logger = LoggerFactory.getLogger(CustomCallAttemptRepository.class);
    private MongoTemplate template;

    @Autowired
    public CustomCallAttemptRepositoryImpl(MongoTemplate template){
        this.template = template;
    }

    @Override
    public List<CallAttempt> getCallAttempts(List<String> checkInIds) {
        if (checkInIds == null || checkInIds.isEmpty()) {
            logger.warn("getCallAttempts with empty check-in IDs list");
            List.of();
        }
        List<String> sanitizedIds = checkInIds.stream().filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
        if (sanitizedIds.isEmpty()) {
            logger.warn("getCallAttempts with only empty check-in ID in the list");
            return List.of();
        }
        Query query = new Query();
        Criteria criteria = Criteria.where("checkInId").in(sanitizedIds);
        query.addCriteria(criteria);
        List<CallAttempt> results = template.find(query, CallAttempt.class);
        return results;
    }
}