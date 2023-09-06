package com.navigatingcancer.healthtracker.api.processor;

import com.navigatingcancer.healthtracker.api.data.CheckInRepositoryTest;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepositoryTest;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.processor.model.HealthTracker;
import com.navigatingcancer.healthtracker.api.processor.model.ProFormatManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { ProFormatManager.class })
public class HealthTrackerTest {

    @Autowired
    private ProFormatManager proFormatManager;
    private SurveyPayload surveyPayload;

    @Test
    public void givenTwoDaysOfCheckIns_whenSeverityHighYesterday_shouldNotSeeHighSeverity() {
        Enrollment e = EnrollmentRepositoryTest.createEnrollment(333, 333, 333);
        List<CheckIn> checkIns = new ArrayList<>();
        CheckIn ci1 = CheckInRepositoryTest.createCheckIn(333, 333, 333, e);
        ci1.setScheduleDate(LocalDate.now().minusDays(1));
        SurveyItemPayload itemPayload = new SurveyItemPayload();
        Map<String, Object> map = new HashMap<>();
        map.put("feverSeverity", "4");
        itemPayload.setPayload(map);
        ci1.setSurveyPayload(itemPayload);

        CheckIn ci2 = CheckInRepositoryTest.createCheckIn(333, 333, 333, e);
        ci2.setScheduleDate(LocalDate.now());
        SurveyItemPayload itemPayload2 = new SurveyItemPayload();
        Map<String, Object> map2 = new HashMap<>();
        map2.put("feverSeverity", "1");
        itemPayload2.setPayload(map2);
        ci2.setSurveyPayload(itemPayload2);
        checkIns.add(ci2);
        checkIns.add(ci1);

        HealthTracker ht = new HealthTracker(e, checkIns, new HealthTrackerStatus(), proFormatManager, surveyPayload);

        int count = ht.getCheckInAggregator().countSideAffects(0, "Severity", "2,3,4");

        Assert.assertEquals(0, count);

    }

    @Test
    public void givenTwoDaysOfCheckIns_whenSeverityHighToday_shouldSeeHighSeverity() {
        Enrollment e = EnrollmentRepositoryTest.createEnrollment(333, 333, 333);
        List<CheckIn> checkIns = new ArrayList<>();
        CheckIn ci1 = CheckInRepositoryTest.createCheckIn(333, 333, 333, e);
        ci1.setScheduleDate(LocalDate.now().minusDays(1));
        SurveyItemPayload itemPayload = new SurveyItemPayload();
        Map<String, Object> map = new HashMap<>();
        map.put("swellingSeverity", "2");
        itemPayload.setPayload(map);
        ci1.setSurveyPayload(itemPayload);

        CheckIn ci2 = CheckInRepositoryTest.createCheckIn(333, 333, 333, e);
        ci2.setScheduleDate(LocalDate.now());
        SurveyItemPayload itemPayload2 = new SurveyItemPayload();
        Map<String, Object> map2 = new HashMap<>();
        map2.put("feverSeverity", "4");
        itemPayload2.setPayload(map2);
        ci2.setSurveyPayload(itemPayload2);
        checkIns.add(ci2);
        checkIns.add(ci1);

        HealthTracker ht = new HealthTracker(e, checkIns, new HealthTrackerStatus(), proFormatManager, surveyPayload);

        int count = ht.getCheckInAggregator().countSideAffects(0, "Severity", "2,3,4");

        Assert.assertEquals(1, count);

    }

}
