package com.navigatingcancer.healthtracker.api.data.repo.proReview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.navigatingcancer.healthtracker.api.data.model.EHRDelivery;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.PdfDeliveryStatus;
import com.navigatingcancer.healthtracker.api.data.model.ProReview;
import com.navigatingcancer.healthtracker.api.rest.exception.RecordNotFoundException;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CustomProReviewRepositoryImplTest {
  @Autowired private ProReviewRepository proReviewRepository;

  private static boolean setUpIsDone = false;
  private EHRDelivery ehrDelivery =
      new EHRDelivery(PdfDeliveryStatus.PENDING_GENERATION, null, "Dr test", new Date(1000L), null);
  private String proId = "111111111111111111111111";

  @Before
  public void setup() {
    if (setUpIsDone) {
      return;
    }

    HealthTrackerStatus htStatus = new HealthTrackerStatus();
    ProReview testReview =
        new ProReview(
            proId, 2L, "3", List.of("4", "5"), null, null, null, ehrDelivery, htStatus, List.of(1));
    this.proReviewRepository.insert(testReview);

    ProReview testReview2 =
        new ProReview(
            "otherId", 2L, "3", List.of("4", "5"), null, null, null, ehrDelivery, htStatus, null);
    this.proReviewRepository.insert(testReview2);
    setUpIsDone = true;
  }

  @Test
  public void updateEhrDeliveryById_modifiesEntry() {
    EHRDelivery ehrDelivery =
        new EHRDelivery(PdfDeliveryStatus.DELIVERED, null, "Dr test", new Date(1000L), null);
    proReviewRepository.updateEhrDeliveryById(proId, ehrDelivery);
  }

  @Test
  public void getNotesByProReviewId_throwsWhenNoneModified() {
    assertThrows(
        RecordNotFoundException.class,
        () -> proReviewRepository.updateEhrDeliveryById("222222222222222222222222", ehrDelivery));
  }

  @Test
  public void getNotesByProReviewId_throwsInvalidArgs() {
    assertThrows(
        IllegalArgumentException.class,
        () -> proReviewRepository.updateEhrDeliveryById("22222", ehrDelivery));
    assertThrows(
        IllegalArgumentException.class,
        () -> proReviewRepository.updateEhrDeliveryById(null, ehrDelivery));
    assertThrows(
        IllegalArgumentException.class,
        () -> proReviewRepository.updateEhrDeliveryById("111111111111111111111111", null));
  }

  @Test
  public void appendPatientActivityId_throwsWithInvalidArgs() {
    assertThrows(
        IllegalArgumentException.class,
        () -> proReviewRepository.appendPatientActivityId("22222", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> proReviewRepository.appendPatientActivityId(null, 45));
  }

  @Test
  public void appendPatientActivityId_doesNotAppendIdIfAlreadyInList() {
    proReviewRepository.appendPatientActivityId(proId, 1);
    ProReview updatedProReview = proReviewRepository.findById(proId).get();
    assertEquals(1, updatedProReview.getPatientActivityIds().size());
  }

  @Test
  public void appendPatientActivityId_doesAppendUniqueIdToList() {
    proReviewRepository.appendPatientActivityId(proId, 5);
    ProReview updatedProReview = proReviewRepository.findById(proId).get();
    assertEquals(2, updatedProReview.getPatientActivityIds().size());
  }
}
