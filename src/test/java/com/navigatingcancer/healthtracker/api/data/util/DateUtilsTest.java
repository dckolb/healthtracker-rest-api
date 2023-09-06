package com.navigatingcancer.healthtracker.api.data.util;

import org.junit.Assert;
import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

public class DateUtilsTest {

    @Test
    public void timeNowInIsoDateFormat() throws ParseException {
        String resultString = DateUtils.timeNowInIsoDateFormat();
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date resultDate = df1.parse(resultString);

        Assert.assertNotNull(resultDate.getTime());
    }

    @Test
    public void validTimeZone() {

        Assert.assertFalse(DateUtils.validTimeZone(null));
        Assert.assertFalse(DateUtils.validTimeZone(""));
        Assert.assertFalse(DateUtils.validTimeZone("America/Hawaii"));
        Assert.assertTrue(DateUtils.validTimeZone("US/Hawaii"));
        Assert.assertTrue(DateUtils.validTimeZone("Pacific/Honolulu"));
    }
}