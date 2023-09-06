package com.navigatingcancer.healthtracker.api.data;

import java.time.LocalDate;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JacksonmappingTest {

    @Test
    public void TestLocalDateMapping() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        LocalDate today = LocalDate.now();
        System.out.println(today);
        try {
            System.out.println(mapper.writeValueAsString(today));
        }
        catch (Exception e){
            System.out.println(e);
        }
        String todaystr = "2018-09-09";
        System.out.println(todaystr);
        System.out.println(mapper.convertValue(todaystr, LocalDate.class));
    }
}
