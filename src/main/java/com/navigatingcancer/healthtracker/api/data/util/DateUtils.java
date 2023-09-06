package com.navigatingcancer.healthtracker.api.data.util;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.TimeZone;

public class DateUtils {

    /**
     * @return string in ISO 8901 format. Example : '2011-12-03T10:15:30Z'
     */
    public static String timeNowInIsoDateFormat() {
        return ZonedDateTime.now( ZoneOffset.UTC ).format( DateTimeFormatter.ISO_INSTANT );
    }

    private static final Set<String> TIMEZONES = Set.of(TimeZone.getAvailableIDs());

    /**
     * @return boolean if input is a valid time zone string
     */
    public static boolean validTimeZone(String timeZone) {
        if(timeZone == null || timeZone.isBlank())
            return false;
        return TIMEZONES.contains(timeZone);
    }

}
