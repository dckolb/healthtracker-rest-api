package com.navigatingcancer.healthtracker.api.data.repo.proReview;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.navigatingcancer.healthtracker.api.data.model.EHRDelivery;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.PdfDeliveryStatus;
import com.navigatingcancer.healthtracker.api.data.model.ProReview;
import com.navigatingcancer.healthtracker.api.rest.exception.RecordNotFoundException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
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

  private EHRDelivery ehrDelivery =
      new EHRDelivery(PdfDeliveryStatus.PENDING_GENERATION, null, "Dr test", new Date(1000L), null);

  private String proId = "111111111111111111111111";
  private String proId2 = "222222222222222222222222";
  private LocalDate yesterday = LocalDate.now().minusDays(1);
  private LocalDate today = LocalDate.now();
  private String enrollmentId = "3";

  @Before
  public void setup() {

    proReviewRepository.deleteAll();
    HealthTrackerStatus htStatus = new HealthTrackerStatus();
    ProReview testReview =
        new ProReview(
            proId,
            2L,
            enrollmentId,
            List.of("4", "5"),
            null,
            null,
            null,
            ehrDelivery,
            htStatus,
            List.of(1));

    testReview.setMostRecentCheckInDate(today);
    this.proReviewRepository.insert(testReview);

    ProReview testReview2 =
        new ProReview(
            "otherId",
            2L,
            enrollmentId,
            List.of("4", "5"),
            null,
            null,
            null,
            ehrDelivery,
            htStatus,
            null);

    testReview2.setMostRecentCheckInDate(yesterday);
    this.proReviewRepository.insert(testReview2);
  }

  @Test
  public void updateEhrDeliveryById_modifiesEntry() {
    EHRDelivery ehrDelivery =
        new EHRDelivery(PdfDeliveryStatus.DELIVERED, null, "Dr test", new Date(1000L), null);
    proReviewRepository.updateEhrDeliveryById(proId, ehrDelivery);
  }

  @Test
  public void updateEhrDeliveryById_throwsWhenNoneModified() {
    assertThrows(
        RecordNotFoundException.class,
        () -> proReviewRepository.updateEhrDeliveryById(proId2, ehrDelivery));
  }

  @Test
  public void updateEhrDeliveryById_throwsInvalidArgs() {
    assertThrows(
        IllegalArgumentException.class,
        () -> proReviewRepository.updateEhrDeliveryById("22222", ehrDelivery));
    assertThrows(
        IllegalArgumentException.class,
        () -> proReviewRepository.updateEhrDeliveryById(null, ehrDelivery));
    assertThrows(
        IllegalArgumentException.class,
        () -> proReviewRepository.updateEhrDeliveryById(proId, null));
  }

  @Test
  public void upsertByLatestCheckInDate_throwsOnInvalidArg() {
    assertThrows(
        IllegalArgumentException.class, () -> proReviewRepository.upsertByLatestCheckInDate(null));
  }

  @Test
  public void upsertByLatestCheckInDate_insertsIfNoneFoundEnrollment() {
    ProReview testProReview = new ProReview();
    testProReview.setMostRecentCheckInDate(today);
    String enrollmentId = "abc";
    testProReview.setEnrollmentId(enrollmentId);
    var currentCount = proReviewRepository.count();
    proReviewRepository.upsertByLatestCheckInDate(testProReview);

    Assert.assertEquals(currentCount + 1, proReviewRepository.count());
  }

  @Test
  public void upsertByLatestCheckInDate_insertsIfNoneFoundDate() {
    ProReview testProReview = new ProReview();
    testProReview.setMostRecentCheckInDate(today);
    testProReview.setEnrollmentId(proId2);
    var currentCount = proReviewRepository.count();
    proReviewRepository.upsertByLatestCheckInDate(testProReview);

    Assert.assertEquals(currentCount + 1, proReviewRepository.count());
  }

  @Test
  public void upsertByLatestCheckInDate_updatesIfFound() {
    List<String> checkInIds = List.of("1", "2");
    ProReview testProReview = new ProReview();
    testProReview.setMostRecentCheckInDate(today);
    testProReview.setEnrollmentId(enrollmentId);
    testProReview.setCheckInIds(checkInIds);
    var currentCount = proReviewRepository.count();
    proReviewRepository.upsertByLatestCheckInDate(testProReview);

    Assert.assertEquals(currentCount, proReviewRepository.count());

    Optional<ProReview> proReviewResult = proReviewRepository.findById(proId);

    Assert.assertTrue(proReviewResult.isPresent());
    Assert.assertEquals(4, proReviewResult.get().getCheckInIds().size());
  }
}
