package com.navigatingcancer.healthtracker.api.data.repo;


import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInStatus;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class CheckInRepositoryTest {

    @Autowired
    private CheckInRepository checkInRepository;

    CheckIn createCheckIn(String enrollmentId,
                          CheckInStatus status,
                          CheckInType checkInType,
                          LocalDate scheduleDate){
        CheckIn ci = new CheckIn();
        ci.setStatus(status);
        ci.setCheckInType(checkInType);
        ci.setEnrollmentId(enrollmentId);
        ci.setScheduleDate(scheduleDate);
        return ci;
    }

    @Test
    public void givenThreeMissedCheckins_shouldFindThree(){
        String enrollmentId = UUID.randomUUID().toString();
        CheckIn ci = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(1));
        this.checkInRepository.insert(ci);

        CheckIn ci2 = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(3));
        this.checkInRepository.insert(ci2);

        CheckIn ci3 = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(5));
        this.checkInRepository.insert(ci3);

        Stream<CheckIn> checkIns = this.checkInRepository.findByEnrollmentIdAndCheckInTypeAndStatusAndScheduleDateGreaterThanOrderByScheduleDateDesc(enrollmentId,
                CheckInType.ORAL,
                CheckInStatus.MISSED,
                LocalDate.now().minusDays(7));

        Assert.assertNotNull(checkIns);

        Assert.assertTrue("should return three", checkIns.count() == 3);

    }

    @Test
    public void givenFourMissedCheckins_shouldFindThreeByDate(){
        String enrollmentId = UUID.randomUUID().toString();
        CheckIn ci = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(1));
        this.checkInRepository.insert(ci);

        CheckIn ci2 = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(3));
        this.checkInRepository.insert(ci2);

        CheckIn ci3 = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(5));
        this.checkInRepository.insert(ci3);

        CheckIn ci4 = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(7));
        this.checkInRepository.insert(ci4);

        Stream<CheckIn> checkIns = this.checkInRepository.findByEnrollmentIdAndCheckInTypeAndStatusAndScheduleDateGreaterThanOrderByScheduleDateDesc(enrollmentId,
                CheckInType.ORAL,
                CheckInStatus.MISSED,
                LocalDate.now().minusDays(7));

        Assert.assertNotNull(checkIns);

        Assert.assertTrue("should return three", checkIns.count() == 3);

    }

    @Test
    public void givenSevenMissedCheckins_shouldFindSixByDate(){
        String enrollmentId = UUID.randomUUID().toString();
        CheckIn ci = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(1));
        this.checkInRepository.insert(ci);

        CheckIn ci2 = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(2));
        this.checkInRepository.insert(ci2);

        CheckIn ci3 = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(3));
        this.checkInRepository.insert(ci3);

        CheckIn ci4 = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(4));
        this.checkInRepository.insert(ci4);

        CheckIn ci5 = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(5));
        this.checkInRepository.insert(ci5);

        CheckIn ci6 = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(6));
        this.checkInRepository.insert(ci6);

        CheckIn ci7 = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(7));
        this.checkInRepository.insert(ci7);

        Stream<CheckIn> checkIns = this.checkInRepository.findByEnrollmentIdAndCheckInTypeAndStatusAndScheduleDateGreaterThanOrderByScheduleDateDesc(enrollmentId,
                CheckInType.ORAL,
                CheckInStatus.MISSED,
                LocalDate.now().minusDays(7));

        Assert.assertNotNull(checkIns);

        Assert.assertTrue("should return three", checkIns.count() == 6);

    }

    @Test
    public void givenFourMissedCheckins_shouldFindNoneByDate(){
        String enrollmentId = UUID.randomUUID().toString();
        CheckIn ci = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(1));
        this.checkInRepository.insert(ci);

        CheckIn ci2 = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(3));
        this.checkInRepository.insert(ci2);

        CheckIn ci3 = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(5));
        this.checkInRepository.insert(ci3);

        CheckIn ci4 = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(7));
        this.checkInRepository.insert(ci4);

        Stream<CheckIn> checkIns = this.checkInRepository.findByEnrollmentIdAndCheckInTypeAndStatusAndScheduleDateGreaterThanOrderByScheduleDateDesc(enrollmentId,
                CheckInType.ORAL,
                CheckInStatus.MISSED,
                LocalDate.now());

        Assert.assertNotNull(checkIns);

        Assert.assertTrue("should return 0", checkIns.count() == 0);

    }


    @Test
    public void givenFourCheckins_shouldFindOneByCriteria(){
        String enrollmentId = UUID.randomUUID().toString();
        CheckIn ci = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(1));
        this.checkInRepository.insert(ci);

        CheckIn ci2 = createCheckIn(enrollmentId, CheckInStatus.MISSED, CheckInType.ORAL, LocalDate.now().minusDays(3));
        this.checkInRepository.insert(ci2);

        CheckIn ci3 = createCheckIn(enrollmentId, CheckInStatus.COMPLETED, CheckInType.ORAL, LocalDate.now().minusDays(5));
        this.checkInRepository.insert(ci3);

        CheckIn ci4 = createCheckIn(enrollmentId, CheckInStatus.COMPLETED, CheckInType.ORAL, LocalDate.now().minusDays(7));
        this.checkInRepository.insert(ci4);

        Stream<CheckIn> checkIns = this.checkInRepository.findTopByEnrollmentIdAndCheckInTypeAndStatusOrderByScheduleDateDesc(enrollmentId,
                CheckInType.ORAL,
                CheckInStatus.COMPLETED);

        Assert.assertNotNull(checkIns);

        CheckIn checkIn = checkIns.findFirst().orElse(null);
        Assert.assertNotNull(checkIn);
        Assert.assertEquals("date isn't right", ci3.getScheduleDate(), checkIn.getScheduleDate());
    }


}
