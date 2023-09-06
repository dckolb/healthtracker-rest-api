package com.navigatingcancer.healthtracker.api.data.repo;

import com.navigatingcancer.healthtracker.api.TestConfig;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepositoryTest;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.LanguageType;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestConfig.class)
public class EnrollmentRepositoryRepoTest {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    public void givenEnrollmentId_shouldFindEnrollment(){
        Enrollment e1 = EnrollmentRepositoryTest.createEnrollment(35l, 35l, 3333l);
        e1.setDefaultLanguage(LanguageType.ES);
        Enrollment e2 = EnrollmentRepositoryTest.createEnrollment(35l, 35l, 3335l);

        this.enrollmentRepository.insert(e1);
        this.enrollmentRepository.insert(e2);

        List<Enrollment> results = this.enrollmentRepository.findEnrollmentsByIds(35l, Arrays.asList(e1.getId()));

        Assert.assertTrue(results.size() == 1);
        Assert.assertEquals(e1.getId(), results.get(0).getId());
        Assert.assertEquals(e1.getDefaultLanguage(), results.get(0).getDefaultLanguage());
        Assert.assertEquals(LanguageType.ES, results.get(0).getDefaultLanguage());
    }
}
