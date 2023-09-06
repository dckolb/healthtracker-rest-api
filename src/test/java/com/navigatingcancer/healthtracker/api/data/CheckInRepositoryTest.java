package com.navigatingcancer.healthtracker.api.data;


import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepositoryTest;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class CheckInRepositoryTest {

  @Autowired private CheckInRepository checkInRepository;

  @Autowired private EnrollmentRepository enrollmentRepository;

  public static CheckIn createCheckIn(
      long locationId, long clinicId, long patientId, Enrollment en) {
    CheckIn ci = new CheckIn();
    ci.setEnrollmentId(en.getId());
    return ci;
  }

  @Test
  public void givenEnrollment_whenFindByEnrollment_thenFindCheckIn() {
    Enrollment e = EnrollmentRepositoryTest.createEnrollment(2, 2, 2);
    enrollmentRepository.save(e); // block here to finish persisting

    CheckIn c = createCheckIn(2, 2, 2, e);
    checkInRepository.save(c);
    List<CheckIn> checkIns = checkInRepository.findByEnrollmentId(e.getId());

    for (CheckIn checkin : checkIns) {
      Assert.assertEquals(c.getEnrollmentId(), checkin.getEnrollmentId());
    }

    checkInRepository.delete(c);
    enrollmentRepository.delete(e);
  }
}
