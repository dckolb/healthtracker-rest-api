package com.navigatingcancer.healthtracker.api.data.model;

import lombok.*;

import com.google.common.base.Strings;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Data
@EqualsAndHashCode(callSuper = false)
public class CheckInSchedule {

    private String medication;
    private LocalDate startDate;
    private LocalDate endDate;
    private CheckInType checkInType;
    private CheckInFrequency checkInFrequency;
    private List<LocalDate> checkInDays;
    private List<Integer> weeklyDays;
    private List<Integer> cycleDays;
    private LocalDate currentCycleStartDate;
    private Integer currentCycleNumber;
    private CheckInScheduleStatus status;

    public Boolean matchesTypeAndMedication(CheckInSchedule data) {
        boolean sameCheckInType = this.getCheckInType() == data.getCheckInType();

        String myMedication = Strings.nullToEmpty(this.getMedication());
        String otherMedication = Strings.nullToEmpty(data.getMedication());

        boolean sameMedication = myMedication.equalsIgnoreCase(otherMedication);

        return sameCheckInType && sameMedication;
    }

    public Boolean matches(CheckInSchedule other) {
        boolean res = matchesTypeAndMedication(other);
        res = res && Objects.equals(getStartDate(), other.getStartDate());
        res = res && Objects.equals(getEndDate(), other.getEndDate());
        res = res && Objects.equals(getCheckInFrequency(), other.getCheckInFrequency());
        res = res && Objects.equals(getCheckInDays(), other.getCheckInDays());
        res = res && Objects.equals(getCycleDays(), other.getCycleDays());
        res = res && Objects.equals(getWeeklyDays(), other.getWeeklyDays());
        return res;
    }

}
