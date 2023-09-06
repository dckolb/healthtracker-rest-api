package com.navigatingcancer.healthtracker.api.data.model;

import java.util.Date;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Document(collection = "pro_review_notes")
public class ProReviewNote {
    private String proReviewId;
    private String content;
    private String createdBy;
    private Date createdDate; 
}