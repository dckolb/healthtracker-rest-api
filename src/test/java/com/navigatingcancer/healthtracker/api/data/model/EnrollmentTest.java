package com.navigatingcancer.healthtracker.api.data.model;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.navigatingcancer.notification.client.domain.SurveyType;

@Slf4j
@RunWith(SpringRunner.class)
public class EnrollmentTest {

    @Test
    public void testGetSurveyType_HealthTracker() {
        Enrollment e = new Enrollment();

        Assert.assertEquals(SurveyType.HEALTHTRACKER, e.getSurveyType());
    }

    @Test
    public void testCurrentCycleNumber() {
        Enrollment e = new Enrollment();
        e.setTxStartDate(LocalDate.now().minusDays(15));
        e.setDaysInCycle(15);

        Assert.assertEquals(2l, e.getCurrentCycleNumber().longValue());
    }

    @Test
    public void testCurrentCycleStartDate() {
        Enrollment e = new Enrollment();
        e.setTxStartDate(LocalDate.now().minusDays(15));
        e.setDaysInCycle(15);

        Assert.assertEquals(LocalDate.now(), e.getCurrentCycleStartDate());
    }

    @Test
    public void testCurrentCycleStartDateToday() {
        Enrollment e = new Enrollment();
        e.setTxStartDate(LocalDate.now());
        e.setDaysInCycle(15);

        Assert.assertEquals(LocalDate.now(), e.getCurrentCycleStartDate());
    }

    @Test
    public void testNextCycleStartDate() {
        Enrollment e = new Enrollment();
        e.setTxStartDate(LocalDate.now().minusDays(15));
        e.setDaysInCycle(15);

        Assert.assertEquals(LocalDate.now().plusDays(15), e.getNextCycleStartDate());
    }

    @Test
    public void testNextCycleStartDateFromToday() {
        Enrollment e = new Enrollment();
        e.setTxStartDate(LocalDate.now());
        e.setDaysInCycle(15);

        Assert.assertEquals(LocalDate.now().plusDays(15), e.getNextCycleStartDate());
    }

    @Test
    public void testNextCycleStartDate_EndOfCycle() {
        Enrollment e = new Enrollment();
        e.setTxStartDate(LocalDate.now().minusDays(15));
        e.setDaysInCycle(15);
        e.setCycles(2);

        Assert.assertEquals(null, e.getNextCycleStartDate());
    }

    @Test
    public void testGetStartDate_OnlyTxStartDate() {
        Enrollment e = new Enrollment();
        LocalDate txStartDate = LocalDate.now().minusDays(15);
        e.setTxStartDate(txStartDate);
        Assert.assertEquals(txStartDate, e.getStartDate());
    }

    @Test
    public void testGetStartDate_OnlyReminderStartDate() {
        Enrollment e = new Enrollment();
        LocalDate reminderStartDate = LocalDate.now().minusDays(15);
        e.setReminderStartDate(reminderStartDate);
        Assert.assertEquals(reminderStartDate, e.getStartDate());
    }

    @Test
    public void testGetStartDate_TxStartDateAndReminderStartDate() {
        Enrollment e = new Enrollment();

        LocalDate txStartDate = LocalDate.now().minusDays(13);
        e.setTxStartDate(txStartDate);

        LocalDate reminderStartDate = LocalDate.now().minusDays(15);
        e.setReminderStartDate(reminderStartDate);

        Assert.assertEquals(reminderStartDate, e.getStartDate());
    }
}
