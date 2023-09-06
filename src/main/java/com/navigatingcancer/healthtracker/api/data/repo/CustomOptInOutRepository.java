package com.navigatingcancer.healthtracker.api.data.repo;

import java.util.List;
import java.util.Map;

import com.navigatingcancer.healthtracker.api.data.model.OptInOut;

public interface CustomOptInOutRepository {
    OptInOut save(OptInOut o);
    List<OptInOut> getOptInOutRecords(Map<String, Object> query);
}