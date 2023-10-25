package com.navigatingcancer.healthtracker.api.data.model;

import com.navigatingcancer.date.utils.DateTimeUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Getter;

public class CheckInDates {
  @Getter private final LocalDateTime nextCheckInDate;

  @Getter private final LocalDateTime lastCheckInDate;

  private CheckInDates(LocalDateTime lastCheckInDate, LocalDateTime nextCheckInDate) {
    this.lastCheckInDate = lastCheckInDate;
    this.nextCheckInDate = nextCheckInDate;
  }

  public static CheckInDates forEnrollment(Enrollment enrollment, List<CheckIn> checkIns) {
    // aggregate summaries for each schedule
    var scheduleSummaries =
        enrollment.getSchedules().stream()
            .map(
                schedule -> {
                  var scopedCheckIns = checkIns.stream().filter(matchSchedule(schedule)).toList();
                  return forSchedule(enrollment, schedule, scopedCheckIns);
                })
            .toList();

    var lastCheckInDate =
        scheduleSummaries.stream()
            .map(CheckInDates::getLastCheckInDate)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);

    var nextCheckInDate =
        scheduleSummaries.stream()
            .map(CheckInDates::getNextCheckInDate)
            .filter(Objects::nonNull)
            .min(Comparator.naturalOrder())
            .orElse(null);

    return new CheckInDates(lastCheckInDate, nextCheckInDate);
  }

  public static CheckInDates forCheckInType(
      Enrollment enrollment, CheckInType checkInType, List<CheckIn> enrollmentCheckIns) {
    if (checkInType == null) {
      return forEnrollment(enrollment, enrollmentCheckIns);
    }

    var checkIns =
        enrollmentCheckIns.stream()
            .filter(c -> checkInType.equals(c.getCheckInType()))
            .collect(Collectors.toList());

    var schedule =
        enrollment.getSchedules().stream()
            .filter(sched -> sched.getCheckInType() == checkInType)
            .findFirst();

    if (schedule.isEmpty()) {
      return forEnrollment(enrollment, checkIns);
    }

    return forSchedule(enrollment, schedule.get(), checkIns);
  }

  public static CheckInDates forSchedule(
      Enrollment enrollment, CheckInSchedule schedule, List<CheckIn> checkIns) {

    // pre-emptively filter check-ins based on schedule
    checkIns = checkIns.stream().filter(matchSchedule(schedule)).toList();

    var latestCheckIn = latestCheckIn(checkIns);
    var lastCheckInDate = latestCheckIn.map(CheckIn::getScheduleDateTime).orElse(null);

    var timeZone = timeZoneFor(enrollment);
    if (timeZone == null) {
      return new CheckInDates(lastCheckInDate, null);
    }

    var fromDate = LocalDate.now(timeZone); // Look from this date onwards
    if (latestCheckIn.isPresent()) {
      // If there are no pending checkins, find the date of the last completed or
      // missed checkin.
      // Use next date as the next checkin date candidate, if it is later than previous
      // candidate
      var latestPlus1Day = latestCheckIn.get().getScheduleDate().plusDays(1);
      if (latestPlus1Day.isAfter(fromDate)) {
        fromDate = latestPlus1Day;
      }
    }

    var nextCheckIn = nextCheckIn(checkIns, fromDate);
    var nextCheckInDate = nextCheckIn.map(CheckIn::getScheduleDateTime).orElse(null);
    if (nextCheckInDate == null && enrollment.getDaysInCycle() != null) {
      var nextScheduledDate = schedule.nextScheduledDate(fromDate, enrollment.getDaysInCycle());
      if (nextScheduledDate != null) {
        nextCheckInDate = LocalDateTime.of(nextScheduledDate, reminderTimeFor(enrollment));
      }
    }

    return new CheckInDates(lastCheckInDate, nextCheckInDate);
  }

  private static Optional<CheckIn> latestCheckIn(List<CheckIn> checkIns) {
    return checkIns.stream()
        .filter(c -> c.getScheduleDate() != null)
        .filter(
            c -> c.getStatus() == CheckInStatus.COMPLETED || c.getStatus() == CheckInStatus.MISSED)
        .max(Comparator.comparing(CheckIn::getScheduleDate));
  }

  private static Optional<CheckIn> nextCheckIn(List<CheckIn> checkIns, LocalDate from) {
    return checkIns.stream()
        .filter(c -> c.getScheduleDate() != null)
        .filter(c -> c.getStatus() == CheckInStatus.PENDING)
        .filter(c -> !c.getScheduleDate().isBefore(from))
        .findFirst();
  }

  private static Predicate<CheckIn> matchSchedule(CheckInSchedule schedule) {
    return checkIn -> {
      if (schedule.getId() != null && checkIn.getCheckInScheduleId() != null) {
        return Objects.equals(checkIn.getCheckInScheduleId(), schedule.getId());
      } else {
        return checkIn.getCheckInType() == schedule.getCheckInType();
      }
    };
  }

  private static LocalTime reminderTimeFor(Enrollment enrollment) {
    return LocalTime.parse(enrollment.getReminderTime(), DateTimeFormatter.ofPattern("H:mm"));
  }

  private static ZoneId timeZoneFor(Enrollment enrollment) {
    if (enrollment.getReminderTime() == null) {
      return null;
    }
    return DateTimeUtils.toZoneId(enrollment.getReminderTimeZone());
  }
}
