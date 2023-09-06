package com.navigatingcancer.healthtracker.api.data.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.OptInOut;
import com.navigatingcancer.healthtracker.api.data.repo.CustomOptInOutRepository;

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
public class OptInOutServiceTest {

    @Autowired
    CustomOptInOutRepository optInOutRepository;

    @Autowired
    OptInOutService optInOutService;

    private static final Long testClinicID = 123l;
    private static final Long testLocationID = 345l;
    private static Long testPatientID = 567l;
    private static final String testSurveyID1 = "survey1";
    private static final String testSurveyID2 = "survey2";

    @Test
    public void testCreateAndFindSingleOptin() {
        testPatientID++;

        OptInOut t1 = new OptInOut();
        t1.setClinicId(testClinicID);
        t1.setLocationId(testLocationID);
        t1.setPatientId(testPatientID);
        t1.setSurveyId(testSurveyID1);
        t1.setAction(OptInOut.Action.OPTED_IN);

        // Save one, make sure the save call works
        OptInOut t2 = optInOutRepository.save(t1);
        Assert.assertNotNull(t2);

        // Find one we just saved making repo call
        Map<String,Object> query = Map.of("clinicId", t1.getClinicId(), "action", t1.getAction());
        List<OptInOut> l1 = optInOutRepository.getOptInOutRecords(query);
        Assert.assertNotNull(l1);
        Assert.assertEquals(1, l1.size());
        Assert.assertEquals(t1.getId(), l1.get(0).getId());

        // Find one we just saved making service call using just the clinic ID
        List<OptInOut> l2 = optInOutService.getOptInOutRecords(t1.getClinicId(), null, null, null, null);
        Assert.assertArrayEquals(l1.toArray(), l2.toArray());

        // Find one we just saved making service call using clinic ID and action filter
        l2 = optInOutService.getOptInOutRecords(t1.getClinicId(), null, null, t1.getAction().name(), null);
        Assert.assertArrayEquals(l1.toArray(), l2.toArray());

        // Find one we just saved making full search call
        l2 = optInOutService.getOptInOutRecords(t1.getClinicId(), t1.getLocationId(), t1.getPatientId(), t1.getAction().name(), t1.getSurveyId());
        Assert.assertArrayEquals(l1.toArray(), l2.toArray());

        // Find nothing using clinic ID and action filter
        l2 = optInOutService.getOptInOutRecords(t1.getClinicId(), null, null, OptInOut.Action.OPTED_OUT.name(), null);
        Assert.assertEquals(0, l2.size());
    }

    @Test
    public void testGetStatus() {
        testPatientID++;

        OptInOut t1 = new OptInOut();
        t1.setClinicId(testClinicID);
        t1.setPatientId(testPatientID);
        t1.setSurveyId(testSurveyID1);
        t1.setAction(OptInOut.Action.OPTED_IN);

        // Save one, make sure the save call works
        t1 = optInOutRepository.save(t1);
        Assert.assertNotNull(t1);

        OptInOut t2 = new OptInOut();
        t2.setClinicId(testClinicID);
        t2.setPatientId(testPatientID);
        t2.setSurveyId(testSurveyID1);
        t2.setAction(OptInOut.Action.OPTED_OUT);
        // Save another one, make sure the save call works
        t2 = optInOutRepository.save(t2);
        Assert.assertNotNull(t2);

        // Now we have two, should get back two with the test call
        var l2 = optInOutService.getOptInOutRecords(testClinicID, null, testPatientID, null, testSurveyID1);
        Assert.assertNotNull(l2);
        Assert.assertEquals(2, l2.size());
        Assert.assertFalse(l2.get(0).getAction()==l2.get(1).getAction());
    }
}