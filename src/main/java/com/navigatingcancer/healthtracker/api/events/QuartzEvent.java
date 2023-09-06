package com.navigatingcancer.healthtracker.api.events;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.navigatingcancer.date.utils.DateTimeUtils;
import com.navigatingcancer.healthtracker.api.data.auth.AuthInterceptor;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.schedule.TriggerPayload;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.CustomCheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerEventsRepository;
import com.navigatingcancer.healthtracker.api.data.repo.HealthTrackerStatusRepository;
import com.navigatingcancer.healthtracker.api.data.service.impl.SchedulingServiceImpl;
import com.navigatingcancer.json.utils.JsonUtils;
import com.navigatingcancer.scheduler.client.domain.SchedulePayload.ScheduleItemPayload;

import lombok.extern.slf4j.Slf4j;

import com.navigatingcancer.scheduler.client.domain.TriggerEvent;

@Slf4j
public abstract class QuartzEvent {

    protected TriggerEvent triggerEvent;
    protected TriggerPayload triggerPayload;
    protected HealthTrackerStatus htStatus;
    protected Enrollment enrollment;
    protected LocalDateTime scheduledDateTime;
    protected CheckInRepository checkInRepository;
    protected CustomCheckInRepository customCheckInRepository;
    protected HealthTrackerStatusRepository healthTrackerStatusRepository;
    protected EnrollmentRepository enrollmentRepository;
	protected HealthTrackerEventsRepository eventsRepository;

    static TriggerPayload getPayloadFromTrigger(TriggerEvent triggerEvent) {
        return JsonUtils.fromJson(triggerEvent.getData(), TriggerPayload.class);
    }

    ZoneId getReminderTimeZoneId() {
        return ZoneId.of(enrollment.getReminderTimeZone());
    }

    LocalTime getReminderTime() {
        return LocalTime.parse(enrollment.getReminderTime(), DateTimeFormatter.ofPattern("H:mm"));
    }

    public Enrollment getEnrollment() {
        return enrollment;
    }

    protected boolean isAutoClosed() {
        boolean onAutoClose = false;
        if (htStatus != null && htStatus.getEndCurrentCycle()
                && triggerPayload.getCheckInType() == CheckInType.SYMPTOM) {
            onAutoClose = true;
        }
        return onAutoClose;
    }

    public Instant getEventInstant() {
        if( triggerEvent != null ) {
            return triggerEvent.getScheduledFireTime().toInstant();
        } else {
            return null;
        }
    }

    protected Boolean isEnrollmentActive() {
        Boolean res = null;
        if (this.enrollment != null) {
            res = this.enrollment.getStatus() == EnrollmentStatus.ACTIVE;
        }
        return res;
    }

    // Create object to act on event actually happening
    protected QuartzEvent(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
        this.triggerEvent = triggerEvent;
        this.checkInRepository = ss.getCheckinRepository();
        this.customCheckInRepository = ss.getCustomCheckinRepository();
        this.healthTrackerStatusRepository = ss.getHealthTrackerStatusRepository();
        this.enrollmentRepository = ss.getEnrollmentRepository();
        this.eventsRepository = ss.getEventsRepository();

        TriggerPayload triggerPayload = getPayloadFromTrigger(triggerEvent);

        this.triggerPayload = triggerPayload;

        String enrollmentId = triggerPayload.getEnrollmentId();
        this.enrollment = ss.getEnrollmentRepository().findById(enrollmentId).get();

        this.scheduledDateTime = DateTimeUtils.toLocalDateTime(triggerEvent.getScheduledFireTime(),
                enrollment.getReminderTimeZone());

        // TODO. Do we relly need get or create? Can't we just get the status?
        this.htStatus = ss.getStatusService().getOrCreateNewStatus(enrollment);
    }

    protected QuartzEvent(Enrollment en) {
        this.enrollment = en;
    }

    // Factory method to create object to act on event
    public static QuartzEvent fromTriggerPayload(TriggerEvent triggerEvent, SchedulingServiceImpl ss) {
        TriggerPayload triggerPayload = getPayloadFromTrigger(triggerEvent);
        switch (triggerPayload.getType()) {
            case SYSTEM:
                return new QuartzSystemEvent(triggerEvent, ss);
            case REMINDER:
                return new QuartzReminderEvent(triggerEvent, ss);
            case STATUS:
                return new QuartzStatusEvent(triggerEvent, ss);
            case ENROLLMENT_END:
                return new EnrollmentEnd(triggerEvent, ss);
            case ENROLLMENT_START:
                return new EnrollmentStart(triggerEvent, ss);
            case CYCLE_END:
                return new CycleEnd(triggerEvent, ss);
            case CYCLE_START:
                return new CycleStart(triggerEvent, ss);
            default:
                log.error("unsupported event type : {}", triggerEvent);
                throw new RuntimeException("unsupported event type");
        }
    }

    // Factory method to create new checkin event to get it scheduled
    public static QuartzEvent newCheckinEvent(TriggerPayload.TriggerType type, Enrollment enrollment, CheckInSchedule checkInSchedule) {
        switch (type) {
            case SYSTEM:
                return new QuartzSystemEvent(enrollment, checkInSchedule);
            case REMINDER:
                return new QuartzReminderEvent(enrollment, checkInSchedule);
            case STATUS:
                return new QuartzStatusEvent(enrollment, checkInSchedule);
            default:
                log.error("not a checkin event type used to create checkin event", type);
                throw new RuntimeException("not a checkin event type used to create checkin event");
        }
    }

    // Factory method to create new enrollment life cycle event to get it scheduled
    public static QuartzEvent newCycleEvent(TriggerPayload.TriggerType type, Enrollment enrollment) {
        switch (type) {
            case CYCLE_START:
                return new CycleStart(enrollment);
            case CYCLE_END:
                return new CycleEnd(enrollment);
            case ENROLLMENT_START:
                return new EnrollmentStart(enrollment);
            case ENROLLMENT_END:
                return enrollment.getSchedulesLastDate() == null ? null : new EnrollmentEnd(enrollment);
            default:
                log.error("not a cycle event type used to create cycle event", type);
                throw new RuntimeException("not a cycle event type used to create cycle event");
        }
    }

    // Function to prepare event record to get persisted
	public HealthTrackerEvent prepareEventRecord() {
		return null; // Do nothing. Status check is not reported as event
    }

    // Make record of the event
    protected void logEvent() {
        HealthTrackerEvent event = prepareEventRecord();
        if( event != null ) {
            Enrollment enr = getEnrollment();
            event.setEnrollmentId(enr.getId());
            event.setClinicId(enr.getClinicId());
            event.setPatientId(enr.getPatientId());
            event.setDate(getEventInstant());
            event.setBy(AuthInterceptor.HEALTH_TRACKER_NAME);
            eventsRepository.save(event);
        }
    }

    // Function called to take actions when event happens
    public void onEvent() {
        logEvent();
    }
    // Function that builds full schedule looking at the event type and frequency
    public abstract ScheduleItemPayload makeSchedule();

}
