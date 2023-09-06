package com.navigatingcancer.healthtracker.api.data.repo.proReviewNote;

import java.util.List;

import com.navigatingcancer.healthtracker.api.data.model.ProReviewNote;

public interface CustomProReviewNoteRepository {
    public List<ProReviewNote> getNotesByProReviewId(String id);
}
