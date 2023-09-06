package com.navigatingcancer.healthtracker.api.data.client;

import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ClinicConfig;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ProgramConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ConfigClientIT {

    @Autowired
    private ConfigServiceClient client;


    @Test
    public void givenExistingClinicConfig_shouldReceive(){
        ClinicConfig result = client.getClinicConfig(12L);
        Assert.assertNotNull(result.getClinicId());
        Assert.assertNull(result.getType());
        Assert.assertEquals("should get clinic config with clinic id",12, result.getClinicId().longValue());
        Assert.assertEquals("should get clinic config with three programs",3, result.getPrograms().size());
    }

    @Test
    public void givenNoExistingClinicConfig_shouldReceiveDefault(){
        ClinicConfig result = client.getClinicConfig(13L);
        Assert.assertNull(result.getClinicId());
        Assert.assertNotNull(result.getType());
        Assert.assertEquals("should get clinic config with three programs",1, result.getPrograms().size());
    }

    @Test
    public void givenProgramWithConsent_shouldParse() {
        ProgramConfig result = client.getProgramConfig("5f065ac67be5761f058b6cc5");
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getConsent());
        ProgramConfig.ProgramConsent consent = result.getConsent();
        Assert.assertNotNull(consent.getConsentContent());
    }
}
