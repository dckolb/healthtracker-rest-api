package com.navigatingcancer.healthtracker.api.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.model.OptInOut;
import com.navigatingcancer.healthtracker.api.data.service.OptInOutService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@Import(TestConfig.class)
public class OptInOutControllerTest {

    @Autowired
    private MockMvc mvc;

    private ObjectMapper mapper;

    @Autowired
    OptInOutService optInOutService;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
    }

    private static final Long testClinicID = 123l;
    private static final Long testLocationID = 345l;
    private static Long testPatientID = 567l;
    private static final String testSurveyID1 = "survey1";
    private static final String testSurveyID2 = "survey2";

    private static final OptInOut makeTestCase(OptInOut.Action a, Long pid, String sid) {
        OptInOut data = new OptInOut();
        data.setClinicId(testClinicID);
        data.setLocationId(testLocationID);
        data.setPatientId(pid);
        data.setSurveyId(sid);
        data.setAction(a);
        return data;
    }

    private static final OptInOut makeTestCase1(OptInOut.Action a, Long pid) {
        return makeTestCase(a, pid, testSurveyID1);
    }

    private void saveAndtestOptInOut(OptInOut.Action a, Long pid) throws Exception {
        OptInOut data = makeTestCase1(a, pid);
        String requestBody = mapper.writeValueAsString(data);

        ResultActions resultAction = mvc.perform(
                MockMvcRequestBuilders.post("/optin").contentType(MediaType.APPLICATION_JSON).content(requestBody));
        resultAction.andDo(print());
        resultAction.andExpect(status().isOk());
        MvcResult mvcResult = resultAction.andReturn();
        String actualResponseBody = mvcResult.getResponse().getContentAsString();
        Assert.assertTrue(actualResponseBody.contains("\"clinicId\":"+testClinicID.toString()));
        Assert.assertTrue(actualResponseBody.contains("\"action\":\"" + a.name() + "\""));
    }

    private String makeGetCall(String url) throws Exception {
        ResultActions resultAction = mvc.perform(
                MockMvcRequestBuilders.get(url).accept(MediaType.APPLICATION_JSON));
        resultAction.andDo(print());
        resultAction.andExpect(status().isOk());
        MvcResult mvcResult = resultAction.andReturn();
        return mvcResult.getResponse().getContentAsString();
    }


    private String makeSearchCall(Long clinicId, Long locationId, Long patientId, String actionFilter, String surveyId) throws Exception {
        String url = "/optin";
        String sep = "?";
        if( clinicId != null ) {
            url = url + sep + "clinicId="+clinicId.toString();
            sep = "&";
        }
        if( patientId != null ) {
            url = url + sep + "patientId="+patientId.toString();
            sep = "&";
        }
        if( locationId != null ) {
            url = url + sep + "locationId="+locationId.toString();
            sep = "&";
        }
        if( actionFilter != null ) {
            url = url + sep + "action="+actionFilter;
            sep = "&";
        }
        if( surveyId != null ) {
            url = url + sep + "surveyId="+surveyId;
            sep = "&";
        }
        return makeGetCall(url);
    }

    private String makeGetStatusCall(Long clinicId, Long patientId, String surveyId) throws Exception {
        String url = "/optin/"+patientId.toString()+"/status";
        String sep = "?";
        if( clinicId != null ) {
            url = url + sep + "clinicId="+clinicId.toString();
            sep = "&";
        }
        if( surveyId != null ) {
            url = url + sep + "surveyId="+surveyId;
            sep = "&";
        }
        return makeGetCall(url);
    }

    @Test
    public void optOutTest() throws Exception {
        testPatientID++;
        // Make sure the save call works
        final OptInOut.Action a = OptInOut.Action.OPTED_OUT;
        saveAndtestOptInOut(a, testPatientID);
        // Make sure the search call works
        String searchResp =  makeSearchCall(testClinicID, testLocationID, testPatientID, a.name(), testSurveyID1);
        Assert.assertTrue(searchResp.contains("\"clinicId\":"+testClinicID.toString()));
        Assert.assertTrue(searchResp.contains("\"action\":\"" + a.name() + "\""));
    }

    @Test
    public void optInTest() throws Exception {
        testPatientID++;
        final OptInOut.Action a = OptInOut.Action.OPTED_IN;
        // Make sure the save call works
        saveAndtestOptInOut(a, testPatientID);
        // Make sure the search call works
        String searchResp =  makeSearchCall(testClinicID, testLocationID, testPatientID, a.name(), testSurveyID1);
        Assert.assertTrue(searchResp.contains("\"clinicId\":"+testClinicID.toString()));
        Assert.assertTrue(searchResp.contains("\"action\":\"" + a.name() + "\""));
    }

    @Test
    public void testSearchSavedViaServiceCall() throws Exception {
        testPatientID++;
        final OptInOut.Action a = OptInOut.Action.OPTED_OUT;
        OptInOut data = makeTestCase1(a, testPatientID);

        OptInOut serviceResponse = optInOutService.save(data);
        Assert.assertNotNull(serviceResponse);
        Assert.assertEquals(serviceResponse.getPatientId(), data.getPatientId());

        String searchResp =  makeSearchCall(testClinicID, testLocationID, testPatientID, a.name(), testSurveyID1);
        Assert.assertTrue(searchResp.contains("\"clinicId\":"+testClinicID.toString()));
        Assert.assertTrue(searchResp.contains("\"action\":\"" + a.name() + "\""));
    }

    @Test
    public void testStatusCall() throws Exception {
        testPatientID++;
        // if opt out not inacted, there is no status
        String searchResp = makeGetStatusCall(testClinicID, testPatientID, testSurveyID1);
        Assert.assertEquals("", searchResp);

        // Do OPT IN
        final OptInOut.Action a = OptInOut.Action.OPTED_IN;
        saveAndtestOptInOut(a, testPatientID);
        // Make sure the status is found and it matches
        searchResp = makeGetStatusCall(testClinicID, testPatientID, testSurveyID1);
        Assert.assertTrue(searchResp.contains('"' + a.name() + '"'));
    }

    @Test
    public void testGetStatus() throws Exception {
        // Advance pid for this test
        testPatientID++;
        // Make one opt in choice
        var a1 = OptInOut.Action.OPTED_IN;
        OptInOut t1 = makeTestCase(a1, testPatientID, testSurveyID1);
        OptInOut serviceResponse = optInOutService.save(t1);
        Assert.assertNotNull(serviceResponse);
        Assert.assertEquals(serviceResponse.getSurveyId(), testSurveyID1);
        // Make sure the opt in/out choice is returned
        String searchResp = makeGetStatusCall(testClinicID, testPatientID, testSurveyID1);
        Assert.assertTrue(searchResp.contains('"'+a1.name()+'"'));

        // Make a different choice
        var a2 = OptInOut.Action.OPTED_OUT;
        OptInOut t2 = makeTestCase(a2, testPatientID, testSurveyID1);
        Thread.sleep(100l); // Make sure some time passes, the choices are ordered by time
        serviceResponse = optInOutService.save(t2);
        Assert.assertNotNull(serviceResponse);
        Assert.assertEquals(serviceResponse.getSurveyId(), testSurveyID1);
        // Make sure it is the last in/out choice that is returned
        searchResp = makeGetStatusCall(testClinicID, testPatientID, testSurveyID1);
        Assert.assertFalse(searchResp.contains('"'+a1.name()+'"'));
        Assert.assertTrue(searchResp.contains('"'+a2.name()+'"'));

        // Make sure both choices are getting listed in the full list
        searchResp =  makeSearchCall(testClinicID, null, testPatientID, null, testSurveyID1);
        Assert.assertTrue(searchResp.contains(a1.name()));
        Assert.assertTrue(searchResp.contains(a2.name()));

    }

}