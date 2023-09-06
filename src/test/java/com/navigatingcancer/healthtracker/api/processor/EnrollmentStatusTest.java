package com.navigatingcancer.healthtracker.api.processor;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload.TriggerType;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.service.CheckInService;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.json.utils.JsonUtils;
import com.navigatingcancer.patientinfo.PatientInfoClient;
import com.navigatingcancer.patientinfo.domain.PatientInfo;
import com.navigatingcancer.scheduler.client.domain.TriggerEvent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class EnrollmentStatusTest {

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    CheckInRepository checkinRepository;

    @Autowired
    EnrollmentService enrollmentService;

    @MockBean
    private PatientInfoClient patientInfoClient;

    @Autowired
    private SchedulingServiceImpl schedulingService;

    private Long patientId = 5L;

    @Before
    public void setup() {
        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setHighRisk(true);
        patientInfo.setId(patientId);

        PatientInfoClient.FeignClient client = Mockito.mock(PatientInfoClient.FeignClient.class);
        Mockito.when(patientInfoClient.getApi()).thenReturn(client);
        Mockito.when(client.getPatients(Mockito.any(), Mockito.any())).thenReturn(Arrays.asList(patientInfo));
    }

    private Enrollment createEnrollment(Long clinicId, Long locationId, Long patientId, SurveyItemPayload... items) {
        return HealthTrackerStatusServiceTest.createEnrollment(enrollmentRepository, checkinRepository, clinicId,
                locationId, patientId, LocalDate.now(), items);
    }

    private Enrollment createEnrollment(Long clinicId, Long locationId, Long patientId, LocalDate dt,
            SurveyItemPayload... items) {
        return HealthTrackerStatusServiceTest.createEnrollment(enrollmentRepository, checkinRepository, clinicId,
                locationId, patientId, dt, items);
    }

    private Enrollment createEnrollment(Long clinicId, Long locationId, Long patientId, LocalDate dt, LocalDate endDate,
            SurveyItemPayload... items) {
        return HealthTrackerStatusServiceTest.createEnrollment(enrollmentRepository, checkinRepository, clinicId,
                locationId, patientId, dt, endDate, items);
    }

    @Test
    public void testNewEnrollmentIsActive() {
        Long clinic1 = 1L;
        Long location1 = 2L;
        Long patient1 = patientId;

        Enrollment e = createEnrollment(clinic1, location1, patient1);

        // Make sure we create new enrollmnent in active state
        Optional<Enrollment> eInRep = enrollmentRepository.findById(e.getId());
        Assert.assertTrue(eInRep.isPresent());
        Assert.assertEquals(EnrollmentStatus.ACTIVE, eInRep.get().getStatus());
    }

    @Test
    public void testContinuousEnrollmentIsActive() {
        Long clinic1 = 1L;
        Long location1 = 2L;
        Long patient1 = patientId + 1;

        // some date in the past
        LocalDate pastDate = LocalDate.of(2019, 01, 01);
        LocalDate enrollmentStartDate = pastDate.minusDays(10);
        LocalDate enrollmentCheckDate = pastDate.plusDays(10);

        Enrollment e = createEnrollment(clinic1, location1, patient1, enrollmentStartDate,
                HealthTrackerStatusServiceTest.createSurvey(CheckInService.MEDICATION_TAKEN_QUESTION_ID,
                        CheckInService.MEDICATION_TAKEN_ANSWER_ID));
        e.setCycles(0); // Set no end to the schedule
        e.setCycleNumber(null);
        e.setReminderTimeZone("America/Los_Angeles");
        e = enrollmentRepository.save(e); // Update enrollment in the DB

        // Verify that there is no end to the schedule
        Assert.assertEquals(0l, (long) e.getCycles());

        // Make sure we create new enrollmnent in active state
        Optional<Enrollment> eInRep = enrollmentRepository.findById(e.getId());
        Assert.assertTrue(eInRep.isPresent());
        Assert.assertEquals(EnrollmentStatus.ACTIVE, eInRep.get().getStatus());

        // Trigger status event, that should invoke updateEnrollmentStatus()
        LocalTime fireTime = LocalTime.MAX;
        TriggerPayload triggerPayload = new TriggerPayload(e.getId(), CheckInType.ORAL, LocalTime.of(9, 0),
                TriggerType.STATUS);
        Date scheduledFireTime = Timestamp.valueOf(LocalDateTime.of(enrollmentCheckDate, fireTime));
        TriggerEvent triggerEvent = new TriggerEvent(JsonUtils.toJson(triggerPayload), scheduledFireTime, null);
        schedulingService.accept(triggerEvent);

        // Get the enrollmnent again, make sure that status did not change
        eInRep = enrollmentRepository.findById(e.getId());
        Assert.assertTrue(eInRep.isPresent());
        Assert.assertEquals(EnrollmentStatus.ACTIVE, eInRep.get().getStatus());
    }

    @Test
    public void testOldEnrollmentGetsDeactivated() {
        Long clinic1 = 1L;
        Long location1 = 2L;
        Long patient1 = patientId + 2;
        Long daysInEnrollment = 3l;

        // some date in the past
        LocalDate pastDate = LocalDate.of(2019, 01, 01);
        LocalDate enrollmentStartDate = pastDate.minusDays(10);
        LocalDate enrollmentCheckDate = pastDate.plusDays(daysInEnrollment + 1); // Some day past the end date

        Enrollment e = createEnrollment(clinic1, location1, patient1, enrollmentStartDate);
        e.setCycles(1); // Make sure it is finite
        e.setCycleNumber(1);
        e.setDaysInCycle(daysInEnrollment.intValue());
        e.setReminderTimeZone("America/Los_Angeles");
        e = enrollmentRepository.save(e); // Update enrollment in the DB

        // Verify that the schedule is finite
        Assert.assertEquals(1L, (long) e.getCycles());
        Assert.assertEquals(daysInEnrollment.longValue(), (long) e.getDaysInCycle());

        // Make sure we create new enrollmnent in active state
        Optional<Enrollment> eInRep = enrollmentRepository.findById(e.getId());
        Assert.assertTrue(eInRep.isPresent());
        Assert.assertEquals(EnrollmentStatus.ACTIVE, eInRep.get().getStatus());

        // Make sure that if we check on some day in the future the enrollment is marked
        // as done
        LocalTime fireTime = LocalTime.MAX;
        TriggerPayload triggerPayload = new TriggerPayload(e.getId(), CheckInType.ORAL, LocalTime.of(9, 0),
                TriggerType.STATUS);
        Date scheduledFireTime = Timestamp.valueOf(LocalDateTime.of(enrollmentCheckDate, fireTime));
        TriggerEvent triggerEvent = new TriggerEvent(JsonUtils.toJson(triggerPayload), scheduledFireTime, null);
        schedulingService.accept(triggerEvent);

        // Get the enrollmnent again, make sure that status changed
        eInRep = enrollmentRepository.findById(e.getId());
        Assert.assertTrue(eInRep.isPresent());
        Assert.assertEquals(EnrollmentStatus.COMPLETED, eInRep.get().getStatus());
    }

    @Test
    public void testEnrollmentCheckOnLastDate() {
        Long clinic1 = 1L;
        Long location1 = 2L;
        Long patient1 = patientId + 3;
        Long daysInCycle = 3l;
        Long cycles = 2l;

        LocalDate someDate = LocalDate.of(2020, 06, 20); // Some date (does not have to be in the past)
        LocalDate enrollmentStartDate = someDate;
        LocalDate enrollmentLastDate = enrollmentStartDate.plusDays(daysInCycle * cycles - 1);

        Enrollment e = createEnrollment(clinic1, location1, patient1, enrollmentStartDate,
                HealthTrackerStatusServiceTest.createSurvey(CheckInService.MEDICATION_TAKEN_QUESTION_ID,
                        CheckInService.MEDICATION_TAKEN_ANSWER_ID));
        e.setCycles(cycles.intValue());
        e.setDaysInCycle(daysInCycle.intValue());
        e.setReminderTimeZone("America/Los_Angeles");
        e = enrollmentRepository.save(e); // Update enrollment in the DB

        // Verify that the schedule is finte
        Assert.assertEquals(daysInCycle.longValue(), (long) e.getDaysInCycle());
        Assert.assertEquals(cycles.longValue(), (long) e.getCycles());

        // Make sure we created new enrollmnent in active state
        Optional<Enrollment> eInRep = enrollmentRepository.findById(e.getId());
        Assert.assertTrue(eInRep.isPresent());
        Assert.assertEquals(EnrollmentStatus.ACTIVE, eInRep.get().getStatus());

        // Make sure previous to last date is treated as ACTIVE
        LocalTime fireTime = LocalTime.MAX;
        TriggerPayload triggerPayload = new TriggerPayload(e.getId(), CheckInType.ORAL, LocalTime.of(9, 0),
                TriggerType.STATUS);
        Date enrollmentPenultimateDateTs = Timestamp
                .valueOf(LocalDateTime.of(enrollmentLastDate.minusDays(1), fireTime));
        TriggerEvent triggerEvent = new TriggerEvent(JsonUtils.toJson(triggerPayload), enrollmentPenultimateDateTs,
                null);
        schedulingService.accept(triggerEvent);

        // Get the enrollmnent again, make sure that status is still active
        eInRep = enrollmentRepository.findById(e.getId());
        Assert.assertTrue(eInRep.isPresent());
        Assert.assertEquals(EnrollmentStatus.ACTIVE, eInRep.get().getStatus());

        // Make sure last date is treated as done
        Date enrollmentLastDateTs = Timestamp.valueOf(LocalDateTime.of(enrollmentLastDate, fireTime));
        triggerEvent = new TriggerEvent(JsonUtils.toJson(triggerPayload), enrollmentLastDateTs, null);
        schedulingService.accept(triggerEvent);

        // Get the enrollmnent again, make sure that status is done now
        eInRep = enrollmentRepository.findById(e.getId());
        Assert.assertTrue(eInRep.isPresent());
        Assert.assertEquals(EnrollmentStatus.COMPLETED, eInRep.get().getStatus());
    }

    @Test
    public void testEnrollmentWithEndDateSetCheckOnLastDate() {
        // This test is the same as the one above except that there is schedule end date
        // set in the enrollment
        Long clinic1 = 1L;
        Long location1 = 2L;
        Long patient1 = patientId + 4;
        Long daysInCycle = 3l;
        Long cycles = 2l;

        LocalDate someDate = LocalDate.of(2020, 06, 20); // Some date (does not have to be in the past)
        LocalDate enrollmentStartDate = someDate;
        LocalDate enrollmentLastDate = enrollmentStartDate.plusDays(daysInCycle * cycles - 1);

        Enrollment e = createEnrollment(clinic1, location1, patient1, enrollmentStartDate, enrollmentLastDate,
                HealthTrackerStatusServiceTest.createSurvey(CheckInService.MEDICATION_TAKEN_QUESTION_ID,
                        CheckInService.MEDICATION_TAKEN_ANSWER_ID));
        e.setCycles(cycles.intValue());
        e.setDaysInCycle(daysInCycle.intValue());
        e.setReminderTimeZone("America/Los_Angeles");
        e = enrollmentRepository.save(e); // Update enrollment in the DB

        // Verify that the schedule is finte
        Assert.assertEquals(daysInCycle.longValue(), (long) e.getDaysInCycle());
        Assert.assertEquals(cycles.longValue(), (long) e.getCycles());

        // Make sure we created new enrollmnent in active state
        Optional<Enrollment> eInRep = enrollmentRepository.findById(e.getId());
        Assert.assertTrue(eInRep.isPresent());
        Assert.assertEquals(EnrollmentStatus.ACTIVE, eInRep.get().getStatus());

        // Make sure previous to last date is treated as ACTIVE
        LocalTime fireTime = LocalTime.MAX;
        TriggerPayload triggerPayload = new TriggerPayload(e.getId(), CheckInType.ORAL, LocalTime.of(9, 0),
                TriggerType.STATUS);
        Date enrollmentPenultimateDateTs = Timestamp
                .valueOf(LocalDateTime.of(enrollmentLastDate.minusDays(1), fireTime));
        TriggerEvent triggerEvent = new TriggerEvent(JsonUtils.toJson(triggerPayload), enrollmentPenultimateDateTs,
                null);
        schedulingService.accept(triggerEvent);

        // Get the enrollmnent again, make sure that status is still active
        eInRep = enrollmentRepository.findById(e.getId());
        Assert.assertTrue(eInRep.isPresent());
        Assert.assertEquals(EnrollmentStatus.ACTIVE, eInRep.get().getStatus());

        // Make sure last date is treated as done
        Date enrollmentLastDateTs = Timestamp.valueOf(LocalDateTime.of(enrollmentLastDate, fireTime));
        triggerEvent = new TriggerEvent(JsonUtils.toJson(triggerPayload), enrollmentLastDateTs, null);
        schedulingService.accept(triggerEvent);

        // Get the enrollmnent again, make sure that status is done now
        eInRep = enrollmentRepository.findById(e.getId());
        Assert.assertTrue(eInRep.isPresent());
        Assert.assertEquals(EnrollmentStatus.COMPLETED, eInRep.get().getStatus());
    }

}
