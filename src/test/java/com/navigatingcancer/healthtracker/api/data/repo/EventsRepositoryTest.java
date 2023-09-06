package com.navigatingcancer.healthtracker.api.data.repo;

import com.navigatingcancer.healthtracker.api.TestConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class EventsRepositoryTest {

    @Autowired
    private HealthTrackerEventsRepository eventsRepository;

    @Test
    public void deDupingTest() {

        Instant now = Instant.now();
        String enrollmentId = UUID.randomUUID().toString();
        Long clinicId = 1l;
        Long patientId = 2l;

        var e1 = HealthTrackerEvent.builder()
            .enrollmentId(enrollmentId).patientId(patientId).clinicId(clinicId)
            .by("clinician")
            .date(now)
            .type(HealthTrackerEvent.Type.ENROLLMENT_ACTIVE)
            .relatedCheckinId(List.of("1","2"))
            .build();
        eventsRepository.save(e1);

        Instant t2 = now.plusSeconds(60l);
        var e2 = HealthTrackerEvent.builder()
            .enrollmentId(enrollmentId).patientId(patientId).clinicId(clinicId)
            .by("clinician")
            .date(t2)
            .type(HealthTrackerEvent.Type.REMINDER_SENT)
            .relatedCheckinId(List.of("1","2"))
            .build();
        eventsRepository.save(e2);

        List<HealthTrackerEvent> el = eventsRepository.getPatientEvents(clinicId, patientId);
        Assert.assertNotNull(el);
        Assert.assertEquals(2, el.size());        

        // Same as e2
        var e3 = HealthTrackerEvent.builder()
            .enrollmentId(enrollmentId).patientId(patientId).clinicId(clinicId)
            .by("clinician")
            .date(t2)
            .type(HealthTrackerEvent.Type.REMINDER_SENT)
            .relatedCheckinId(List.of("1","2"))
            .build();
        eventsRepository.save(e3);

        // The data size should not change, we skip duplicated reminders
        List<HealthTrackerEvent> el2 = eventsRepository.getPatientEvents(clinicId, patientId);
        Assert.assertNotNull(el2);
        Assert.assertEquals(el.size(), el2.size());        
    }
}
