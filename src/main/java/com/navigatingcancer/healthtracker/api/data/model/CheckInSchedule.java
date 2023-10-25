package com.navigatingcancer.healthtracker.api.data.model;

import com.google.common.base.Strings;
import com.navigatingcancer.healthtracker.api.data.model.scheduleConfig.ScheduleConfig;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
public class CheckInSchedule extends AbstractDocument {

  /** @deprecated Use surveyInstance surveyParams instead */
  @Deprecated private String medication;

  /** @deprecated Use surveyInstance surveyId instead */
  @Deprecated private CheckInType checkInType;

  private String surveyInstanceId;
  private Map<String, Object> scheduleParameters;
  private ScheduleType scheduleType;
  private ScheduleConfig scheduleConfig;
  private LocalDate startDate;
  private LocalDate endDate;

  /** @deprecated Use scheduleConfig instead */
  @Deprecated private CheckInFrequency checkInFrequency;

  private List<LocalDate> checkInDays;
  @Deprecated private List<Integer> weeklyDays;
  private List<Integer> cycleDays;
  private LocalDate currentCycleStartDate;
  private Integer currentCycleNumber;
  private Integer cycles;
  private Integer daysInCycle;
  /* End use scheduleConfig */

  /* Should these live in scheduleParameters?  Which do we really need? */
  private LocalDate reminderStartDate;
  private LocalDate patientReportedTxStartDate;

  private CheckInScheduleStatus status;
  private String enrollmentId;

  /**
   * @deprecated compare using surveyInstance instead
   * @param data
   * @return
   */
  @Deprecated
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

  public static LocalDate getLastScheduledDate(Collection<CheckInSchedule> schedules) {
    return schedules.stream()
        .map(CheckInSchedule::getEndDate)
        .filter(Objects::nonNull)
        .max(Comparator.comparing(LocalDate::toEpochDay))
        .orElse(null);
  }

  /**
   * Returns the next scheduled date according to this schedule's configuration.
   *
   * <p>Note: This implementation ignores the schedule's `daysInCycle` property; it must be provided
   * as an argument. As of implementation, `daysInCycle` hadn't been migrated from the enrollment.
   *
   * @param from
   * @param daysInCycle
   * @return
   */
  public LocalDate nextScheduledDate(LocalDate from, Integer daysInCycle) {
    LocalDate date = from;
    for (int i = 0; i < daysInCycle; i++) {
      if (isScheduled(date, daysInCycle)) {
        return date;
      }

      date = date.plusDays(1);
    }
    return null;
  }

  /**
   * Returns whether the provided date is scheduled for a check-in, according to this schedule's
   * configuration
   *
   * @param date
   * @return
   */
  public boolean isScheduled(LocalDate date) {
    return isScheduled(date, getDaysInCycle());
  }

  /**
   * Returns whether the provided date is scheduled for a check-in, according to this schedule's
   * configuration
   *
   * @param date
   * @param daysInCycle override for this schedule's daysInCycle value -- supports backward
   *     compatibility where this schedule's daysInCycle is null
   * @return
   */
  public boolean isScheduled(LocalDate date, Integer daysInCycle) {
    if (getEndDate() != null && getEndDate().isBefore(date)) {
      return false;
    }

    if (getStartDate() != null && getStartDate().isAfter(date)) {
      return false;
    }

    boolean result = false;

    // convert to Calendar style where week starts from Sunday
    int dayOfWeek;

    if (date.getDayOfWeek().getValue() == 7) {
      dayOfWeek = 1;
    } else {
      dayOfWeek = date.getDayOfWeek().getValue() + 1;
    }

    if (getCheckInFrequency() == CheckInFrequency.DAILY) {
      result = true;
    } else if (getCheckInFrequency() == CheckInFrequency.WEEKDAY) {
      result = dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY;
    } else if (getCheckInFrequency() == CheckInFrequency.WEEKLY) {
      result = getWeeklyDays().contains(dayOfWeek);
    } else if (getCheckInFrequency() == CheckInFrequency.CUSTOM) {
      long daysDiff =
          Duration.between(getStartDate().atStartOfDay(), date.atStartOfDay()).toDays() + 1;

      int dayNumber = (int) (daysDiff % daysInCycle);
      // check if this is the last day of the cycle
      if (dayNumber == 0) {
        dayNumber = daysInCycle;
      }

      result = getCycleDays().contains(dayNumber);
    }

    return result;
  }
}
