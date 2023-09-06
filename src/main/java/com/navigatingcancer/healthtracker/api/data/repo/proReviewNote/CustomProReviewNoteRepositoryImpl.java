package com.navigatingcancer.healthtracker.api.data.repo.proReviewNote;

import com.navigatingcancer.healthtracker.api.data.model.ProReviewNote;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class CustomProReviewNoteRepositoryImpl implements CustomProReviewNoteRepository {
  private MongoTemplate template;

  @Autowired
  public CustomProReviewNoteRepositoryImpl(MongoTemplate template) {
    this.template = template;
  }

  public List<ProReviewNote> getNotesByProReviewId(String id) {
    if (id == null || id.isEmpty()) {
      throw new IllegalArgumentException("Null or empty id provided to getProReviewById");
    }

    Query query = new Query();
    Criteria criteria = Criteria.where("proReviewId").is(id);
    query.addCriteria(criteria);
    query.with(Sort.by(Sort.Direction.DESC, "createdDate"));
    List<ProReviewNote> notes = this.template.find(query, ProReviewNote.class);

    return notes;
  }
}
