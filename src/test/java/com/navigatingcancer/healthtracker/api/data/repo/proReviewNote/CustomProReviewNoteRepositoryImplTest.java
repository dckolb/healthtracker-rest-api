package com.navigatingcancer.healthtracker.api.data.repo.proReviewNote;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.navigatingcancer.healthtracker.api.data.model.ProReviewNote;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CustomProReviewNoteRepositoryImplTest {
  @Autowired private ProReviewNoteRepository proReviewNoteRepository;

  @Test
  public void getNotesByProReviewId_buildsCorrectQuery() {
    String testId = UUID.randomUUID().toString();
    ProReviewNote testNote = new ProReviewNote(testId, "hello", "dr Test", new Date(1000L));
    this.proReviewNoteRepository.insert(testNote);

    ProReviewNote testNote2 = new ProReviewNote(testId, "unique text", "dr no", new Date(2000L));
    this.proReviewNoteRepository.insert(testNote2);

    ProReviewNote otherNote = new ProReviewNote("otherId", "bye", "dr no", new Date(1L));
    this.proReviewNoteRepository.insert(otherNote);

    List<ProReviewNote> returnNotes = proReviewNoteRepository.getNotesByProReviewId(testId);

    Assert.assertNotNull(returnNotes);
    Assert.assertTrue("returns both notes", returnNotes.size() == 2);
    Assert.assertTrue(
        "returns correct note", testNote2.toString().equals(returnNotes.get(0).toString()));
  }

  @Test
  public void getNotesByProReviewId_rejectsEmptyAndNullId() {
    assertThrows(
        IllegalArgumentException.class, () -> proReviewNoteRepository.getNotesByProReviewId(null));

    assertThrows(
        IllegalArgumentException.class, () -> proReviewNoteRepository.getNotesByProReviewId(""));
  }
}
