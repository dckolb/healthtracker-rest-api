package com.navigatingcancer.healthtracker.api.data.repo;

import static org.springframework.data.mongodb.core.query.Query.query;

import java.util.List;
import java.util.Map;

import com.navigatingcancer.healthtracker.api.data.model.OptInOut;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
public class CustomOptInOutRepositoryImpl implements CustomOptInOutRepository {

    private MongoTemplate mongoTemplate;

    @Autowired
    public CustomOptInOutRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public OptInOut save(OptInOut o) {
        return mongoTemplate.save(o);
    }

    @Override
    public List<OptInOut> getOptInOutRecords(Map<String, Object> params) {
        Criteria criteria = new Criteria();
        params.forEach((k, v) -> criteria.and(k).is(v));
        return mongoTemplate.query(OptInOut.class).matching(query(criteria)).all();
    }

}