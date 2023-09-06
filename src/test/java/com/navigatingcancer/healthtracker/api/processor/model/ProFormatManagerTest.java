package com.navigatingcancer.healthtracker.api.processor.model;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.TherapyType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = {ProFormatManager.class})
public class ProFormatManagerTest {

    @Value(value = "${MN.clinicIds:291}")
	private Long[] MN_CLINIC_IDs;

    private Long mnClinicId;

    @Autowired
    private ProFormatManager proFormatManager;

    @Before
	public void setup() {
		mnClinicId = MN_CLINIC_IDs[0];
	}

    @Test
    public void whenInvalidClinicIdOrTherapyType_thenReturnsFalse() {
        Enrollment enrollment = new Enrollment();
        enrollment.setClinicId(null);

        Assert.assertFalse("Null clinic id", proFormatManager.followsCtcaeStandard(enrollment));

        enrollment.setClinicId(1L);
        enrollment.setTherapyTypes(null);
        Assert.assertFalse("Null therapy types", proFormatManager.followsCtcaeStandard(enrollment));
    }

    @Test
    public void whenClinicIsMnAndTypeIsVI_thenFollowsCtcaeStandard() {
        Set<TherapyType> mnTherapyTypes = new HashSet<>();
        mnTherapyTypes.add(TherapyType.IV);
        mnTherapyTypes.add(TherapyType.ORAL);

        Enrollment enrollment = new Enrollment();
        enrollment.setClinicId(mnClinicId);
        enrollment.setTherapyTypes(mnTherapyTypes);
        assertTrue("MN clinic with IV treatment resultType follows Ctcae standard",
                proFormatManager.followsCtcaeStandard(enrollment));
    }

    @Test
    public void whenClinicIsMnAndTypeIsNotVI_thenDoesNotFollowCtcaeStandard() {
        Set<TherapyType> therapyTypes = new HashSet<>();
        therapyTypes.add(TherapyType.ORAL);
        Enrollment enrollment = new Enrollment();
        enrollment.setClinicId(mnClinicId);
        enrollment.setTherapyTypes(therapyTypes);

        assertFalse("MN clinic with Non-IV treatment resultType does not follow Ctcae standard",
                proFormatManager.followsCtcaeStandard(enrollment));
    }

    @Test
    public void whenClinicIsNotMnAndTypeIsVI_thenDoesNotFollowCtcaeStandard() {
        Long otherClinicId = 000L;

        Set<TherapyType> therapyTypes = new HashSet<>();
        therapyTypes.add(TherapyType.IV);
        Enrollment enrollment = new Enrollment();
        enrollment.setClinicId(otherClinicId);
        enrollment.setTherapyTypes(therapyTypes);

        assertFalse("Non-MN clinic with IV treatment resultType does not follow Ctcae standard",
                proFormatManager.followsCtcaeStandard(enrollment));
    }
}