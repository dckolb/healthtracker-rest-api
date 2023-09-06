package com.navigatingcancer.healthtracker.api.data.model;

import java.util.Date;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.query.Update;

import lombok.Data;

@Data
public class AbstractDocument {

    private @Id
    String id;
    @Version
    private Long version;
    @CreatedDate
    private Date createdDate;
    @CreatedBy
    private String createdBy;
    @LastModifiedDate
    private Date updatedDate;
    @LastModifiedBy
    private String updatedBy;

    public static Update appendUpdatedBy(Update u) {
        Date d = new Date();
        u.set("updatedDate", d)
        .set("updatedBy", "Health Tracker")
        .setOnInsert("createdDate", d)
        .setOnInsert("createdBy", "Health Tracker");
        return u;
    }

}
