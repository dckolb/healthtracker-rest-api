package com.navigatingcancer.healthtracker.api.data.service.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CheckInScheduleServiceImplTest {
  @Autowired CheckInScheduleServiceImpl checkInScheduleService;

  @MockBean CheckInRepository checkInRepository;

  @MockBean EnrollmentRepository enrollmentRepository;

  Enrollment enrollment;
  CheckInSchedule schedule;
  CheckIn checkIn;

  @Before
  public void setup() {
    schedule = new CheckInSchedule();
    schedule.setId("my_schedule");
    schedule.setCheckInType(CheckInType.ORAL);

    enrollment = new Enrollment();
    enrollment.setId("my_enrollment");
    enrollment.setSchedules(List.of(schedule));

    checkIn = new CheckIn();
    checkIn.setId("my_checkIn");
    checkIn.setCheckInScheduleId(schedule.getId());
    checkIn.setCheckInType(schedule.getCheckInType());

    when(checkInRepository.findCheckInsBySchedule("my_enrollment", schedule))
        .thenReturn(List.of(checkIn));
    when(enrollmentRepository.findById("my_enrollment")).thenReturn(Optional.of(enrollment));
  }

  @Test
  public void testGetCheckInDetailsBySchedule() {
    var details =
        checkInScheduleService.getCheckInDetailsBySchedule(
            "my_enrollment", "my_schedule", Optional.empty());

    assertEquals("my_schedule", details.getCheckInScheduleId());
    assertEquals("my_enrollment", details.getEnrollmentId());
    assertNotNull(details.getCheckIns());
    assertEquals(checkIn.getId(), details.getCheckIns().get(0).getId());
    assertNull(details.getCheckIns().get(0).getCheckInSchedule());
  }

  @Test
  public void testGetCheckInDetailsBySchedule_checkInType() {
    var details =
        checkInScheduleService.getCheckInDetailsBySchedule(
            "my_enrollment", CheckInType.ORAL, Optional.empty());
    assertEquals("my_schedule", details.getCheckInScheduleId());
    assertEquals("my_enrollment", details.getEnrollmentId());
    assertNotNull(details.getCheckIns());
    assertEquals(checkIn.getId(), details.getCheckIns().get(0).getId());
    assertNull(details.getCheckIns().get(0).getCheckInSchedule());
  }
}
