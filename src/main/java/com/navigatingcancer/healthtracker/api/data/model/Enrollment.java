package com.navigatingcancer.healthtracker.api.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.navigatingcancer.healthtracker.api.data.validator.TimeZoneValidator;
import com.navigatingcancer.notification.client.domain.SurveyType;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;

import static java.time.temporal.ChronoUnit.DAYS;

@Data
@EqualsAndHashCode(callSuper = false)
@Document(collection = "enrollments")
public class Enrollment extends AbstractDocument {

    private @NotNull Long patientId;
    private @NotNull Long clinicId;
    private Long locationId;
    private Long providerId;
    private Long assigneeId;
    private EnrollmentStatus status;
    private String emailAddress;
    private String phoneNumber;
    private @NotNull Boolean alerts;
    private String medication;
    private LocalDate txStartDate;
    private LocalDate reminderStartDate;
    private LocalDate endDate;
    private Integer cycles;
    private Integer cycleNumber;
    private @NotNull Integer daysInCycle;
    private Integer repeat;
    private @NotNull String reminderTime;
    private @NotNull @TimeZoneValidator String reminderTimeZone;
    private @NotEmpty List<CheckInSchedule> schedules;
    private String url;
    private List<EnrollmentStatusLog> statusLogs = new ArrayList<>();
    private Set<TherapyType> therapyTypes;
    private boolean manualCollect;
    private String manualCollectNote;
    private Boolean symptomScheduleMatchesOral;
    private LanguageType defaultLanguage = LanguageType.EN;
    private String surveyId;
    private String consentRequestId;
    private String programId;
    private String consentStatus;
    private Date consentUpdatedDate;
    private LocalDate patientReportedTxStartDate;
    private LocalDate firstCheckInResponseDate;

    @JsonProperty
    public boolean isConsentRequirementMet(){
        return this.consentRequestId == null ||
                "COMPLETED".equalsIgnoreCase(this.consentStatus);
    }

    @JsonProperty
    public SurveyType getSurveyType() {
        return SurveyType.HEALTHTRACKER;
    }

    @JsonProperty
    public Long getCurrentCycleNumber() {
        LocalDate startDate = this.getStartDate();
        if (startDate == null) return null;
        Long daysFromStart = DAYS.between(startDate.atStartOfDay(), LocalDate.now().atStartOfDay());
        if(daysFromStart < 0){
            return 0L;
        }
        return daysFromStart / this.daysInCycle + 1;
    }

    @JsonProperty
    public LocalDate getNextCycleStartDate() {
        if(this.getCurrentCycleNumber() == null) {
            return null;
        }
        if (this.cycles == null || this.cycles == 0 || this.cycles > this.getCurrentCycleNumber()) {
            return this.getCurrentCycleStartDate().plusDays(this.daysInCycle);
        } else {
            // no more cycles
            return null;
        }
    }

    @JsonProperty
    public LocalDate getCurrentCycleStartDate() {
        LocalDate startDate = this.getStartDate();
        if (startDate == null) return null;

        Long daysFromStart = DAYS.between(startDate.atStartOfDay(), LocalDate.now().atStartOfDay());
        if(daysFromStart < 0){
            return this.getStartDate();
        }
        Long cycleDays = daysFromStart % this.daysInCycle;
        return LocalDate.now().minusDays(cycleDays);
    }

    public LocalDate getStartDate() {
        if (this.reminderStartDate != null) {
            return this.reminderStartDate;
        }

        return this.txStartDate;
    }

    // Make sure each schedule has at least the start date set
    // and end date if this is a finite schedule
    @JsonIgnore
    public void validateDates() {
        final LocalDate startDate = getTxStartDate() != null ? getTxStartDate() : getReminderStartDate();
        final Integer days = getCycles() != null && getCycles() > 0 ? getDaysInCycle() * getCycles() : null;
        getSchedules().forEach(checkInSchedule -> {
            if (checkInSchedule.getStartDate() == null) {
                checkInSchedule.setStartDate(startDate);
            }
            if (checkInSchedule.getEndDate() == null && checkInSchedule.getStartDate() != null && days != null) {
                checkInSchedule.setEndDate(checkInSchedule.getStartDate().plusDays(days - 1));
            }
        });
    }

    // Return latest end day found in any schedules.
    // Note that the last day of the schedule usually is set by the validateDates() call
    // Make sure this function is called when end dates are set, if any.
    @JsonIgnore
    public LocalDate getSchedulesLastDate() {
        LocalDate res = null;
        if (cycles != null && cycles.intValue() > 0) {
            Optional<LocalDate> endDate = getSchedules().stream().filter(s -> s.getEndDate() != null)
                    .map(s -> s.getEndDate()).max(Comparator.comparing(LocalDate::toEpochDay));
            res = endDate.isPresent() ? endDate.get() : null;
        }
        return res;
    }

    public String getUrl(){
        if (this.url == null) return "";

        // for backwards compatibility some enrollments have the language saved as part
        // of the url already, so strip it out then replace it with the updated one
        if (this.url.contains("?language")) {
            return this.url.substring(0, this.url.indexOf("?language")) + "?language=" + this.getDefaultLanguage().toString().toLowerCase();
        }
        return this.url  + "?language=" + this.getDefaultLanguage().toString().toLowerCase();
    }

    // Return a descriptive list of changes that may be of interest to users
    public List<String> diffDescr(Enrollment other) {
        List<String> res = new LinkedList<>();
        if (!Objects.equals(getLocationId(), other.getLocationId())) {
            res.add("Location");
        }
        if (!Objects.equals(getProviderId(), other.getProviderId())) {
            res.add("Provider");
        }
        if (!Objects.equals(getAssigneeId(), other.getAssigneeId())) {
            res.add("Assignee");
        }
        if (!Objects.equals(isManualCollect(), other.isManualCollect())) {
            res.add("Will the patient be completing self check-ins");
        }
        if (!Objects.equals(getPhoneNumber(), other.getPhoneNumber())) {
            res.add("Phone Number");
        }
        if (!Objects.equals(getDefaultLanguage(), other.getDefaultLanguage())) {
            res.add("Language Preference");
        }
        if (!Objects.equals(getTherapyTypes(), other.getTherapyTypes())) {
            res.add("Treatment");
        }
        if (!Objects.equals(getReminderTimeZone(), other.getReminderTimeZone())) {
            res.add("Patient Time Zone");
        }
        if (!Objects.equals(getReminderTime(), other.getReminderTime())) {
            res.add("Reminder Time");
        }
        if (!Objects.equals(getTxStartDate(), other.getTxStartDate())) {
            res.add("Treatment start date");
        }
        if (!Objects.equals(getReminderStartDate(), other.getReminderStartDate())) {
            res.add("Reminder start date");
        }
        if (!Objects.equals(getCycles(), other.getCycles())) {
            res.add("End schedule after cycle");
        }
        if (!Objects.equals(getDaysInCycle(), other.getDaysInCycle())) {
            res.add("Days in cycle");
        }
        if (!Objects.equals(getSchedules(), other.getSchedules())) {
            List<CheckInSchedule> s1 = getSchedules();
            List<CheckInSchedule> s2 = other.getSchedules();
            boolean scheduleEquals = s1.size() == s2.size();
            if (scheduleEquals) {
                scheduleEquals = IntStream.range(0, s1.size()).allMatch(i -> s1.get(i).matches(s2.get(i)));
            }
            if (!scheduleEquals) {
                // TODO. Add detailed schedule differences?
                res.add("Schedules");
            }
        }
        return res;
    }
    public String getSurveyId() {
        if (surveyId == CheckIn.ORAL_ADHERENCE_HT_PX && manualCollect == true) {
            return CheckIn.ORAL_ADHERENCE_HT_CX;
        }
        if (surveyId == CheckIn.ORAL_ADHERENCE_PX && manualCollect == true) {
            return CheckIn.ORAL_ADHERENCE_CX;
        }
        if (surveyId == CheckIn.HEALTH_TRACKER_PX && manualCollect == true) {
            return CheckIn.HEALTH_TRACKER_CX;
        }
        return surveyId;
    }
}
