package com.navigatingcancer.healthtracker.api.data.service.impl;

import static com.navigatingcancer.healthtracker.api.data.auth.AuthInterceptor.HEALTH_TRACKER_NAME;

import com.google.common.base.Strings;
import com.navigatingcancer.date.utils.DateTimeUtils;
import com.navigatingcancer.healthtracker.api.data.auth.IdentityContext;
import com.navigatingcancer.healthtracker.api.data.auth.IdentityContextHolder;
import com.navigatingcancer.healthtracker.api.data.client.PatientInfoServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyId;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ClinicConfig;
import com.navigatingcancer.healthtracker.api.data.repo.*;
import com.navigatingcancer.healthtracker.api.data.service.*;
import com.navigatingcancer.healthtracker.api.events.HealthTrackerEventsPublisher;
import com.navigatingcancer.healthtracker.api.metrics.HealthTrackerCounterMetric;
import com.navigatingcancer.healthtracker.api.metrics.MetersService;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.healthtracker.api.processor.model.ProFormatManager;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import com.navigatingcancer.healthtracker.api.rest.exception.BadDataException;
import com.navigatingcancer.healthtracker.api.rest.exception.RecordNotFoundException;
import com.navigatingcancer.healthtracker.api.rest.representation.CheckInResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CheckInServiceImpl implements CheckInService {

  @Autowired private CheckInRepository checkInRepository;

  @Autowired private PracticeCheckInRepository practiceCheckInRepository;

  @Autowired private EnrollmentRepository enrollmentRepository;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private SchedulingServiceImpl schedulingService;

  @Autowired private HealthTrackerStatusService healthTrackerStatusService;

  @Autowired private PatientInfoServiceClient patientInfoClient;

  @Autowired private ProFormatManager proFormatManager;

  @Autowired private IdentityContextHolder identityContextHolder;

  @Autowired private PatientInfoService patientInfoService;

  @Autowired private SurveyConfigService surveyConfigService;

  @Autowired private CheckInCreationService checkInCreationService;

  @Autowired private HealthTrackerEventsPublisher healthTrackerEventsPublisher;

  @Autowired private MetersService metersService;

  private static final String BACKFILL_DATE = "medicationTakenDate";

  /**
   * Called when check-in survey (PX) is saved
   *
   * @return list of checkins derived from the given payload
   */
  @Override
  public List<CheckIn> checkIn(SurveyPayload surveyPayload) {
    log.debug("checkIn with SurveyPayload {}", surveyPayload);

    // Persist ORAL check-in
    List<CheckIn> oralCheckins = new LinkedList<>();
    Enrollment oralEnrollment =
        processSurveyItemPayloads(surveyPayload, surveyPayload.getOral(), oralCheckins);
    // Persist symptom (side-affect) check-in
    List<CheckIn> checkins = new LinkedList<>(oralCheckins);
    Enrollment symptomEnrollment =
        processSurveyItemPayloads(surveyPayload, surveyPayload.getSymptoms(), checkins);
    // NOTE: both must be the same, legacy code expected that may not be the case,
    // not sure why
    Enrollment enrollment = oralEnrollment == null ? symptomEnrollment : oralEnrollment;

    String enrollmentId = null;
    if (enrollment != null) {
      enrollmentId = enrollment.getId();
    } else {
      String msg =
          MessageFormatter.format("no enrollment found for checkin {}", surveyPayload).getMessage();
      log.error(msg);
      // If no valid data is provided, bail
      throw new BadDataException(msg);
    }

    log.info(
        "oralEnrollment {} symptomEnrollment {} enrollment {}",
        oralEnrollment,
        symptomEnrollment,
        enrollmentId);

    // if reschedule call is made it should be after all checkins are saved.
    // otherwise DB updates concurrency can cause save failures
    rescheduleIfMedsDateChange(oralCheckins, enrollment);

    healthTrackerEventsPublisher.publishCheckinCompleted(enrollment, checkins, surveyPayload);
    healthTrackerStatusService.push(enrollmentId, surveyPayload, checkins);
    // Send metric to DD
    Long clinicId = enrollment != null ? enrollment.getClinicId() : 0;
    metersService.incrementCounter(clinicId, HealthTrackerCounterMetric.CHECKIN_COMPLETED);

    return checkins;
  }

  @Override
  public CheckIn checkInBackfill(CheckIn backfillCheckin) {
    // Create checkin (likely) in the past. Use insert. We want to create a new
    // object. Not to update existing one.
    backfillCheckin.setId(null);
    log.debug("inserting backfill check in: {}", backfillCheckin);
    return checkInRepository.insert(backfillCheckin);
  }

  @Override
  public List<CheckInData> getCheckInDataByEnrollmentIDs(List<String> enrollmentIds) {
    return getCheckInDataByEnrollmentIDs(enrollmentIds, true);
  }

  @Override
  public List<CheckInData> getCheckInDataByEnrollmentIDs(
      List<String> enrollmentIds, boolean includePatientInfo) {

    EnrollmentQuery query = new EnrollmentQuery();
    query.setId(enrollmentIds);
    query.setAll(true);
    List<Enrollment> enrollments = enrollmentRepository.findEnrollments(query);
    this.enrollmentService.setProgramIds(enrollments);

    List<CheckInData> checkInDataList = new ArrayList<>();
    for (Enrollment enrollment : enrollments) {
      CheckInData checkInData = new CheckInData();

      // NOTE : this makes about 8-10 mongodb calls per enrollment
      // if this performs poorly we should update the queries to handle multiple
      // enrollments at a
      // time
      getCheckInData(checkInData, enrollment, includePatientInfo);
      checkInDataList.add(checkInData);
    }
    return checkInDataList;
  }

  @Override
  public CheckInData getCheckInData(String enrollmentId) {
    Enrollment enrollment = enrollmentRepository.findById(enrollmentId).get();
    List<Enrollment> enrollments = new ArrayList<>();
    enrollments.add(enrollment);
    this.enrollmentService.setProgramIds(enrollments);
    return getCheckInData(new CheckInData(), enrollments.get(0), true);
  }

  /**
   * Returns data for PX NOTE 1 : checks other statuses if query returns 0 enrollments. NOTE 2 :
   * returns first checkIn found FIXME : Rename method to represent this behaviour better ?
   */
  @Override
  public CheckInData getCheckInData(EnrollmentQuery query) {

    CheckInData checkInData = new CheckInData();

    List<Enrollment> enrollments = enrollmentRepository.findEnrollments(query);
    this.enrollmentService.setProgramIds(enrollments);

    if (enrollments == null || enrollments.isEmpty()) {
      // check for other states
      query.setStatus(
          Arrays.asList(
              EnrollmentStatus.COMPLETED, EnrollmentStatus.PAUSED, EnrollmentStatus.STOPPED));
      enrollments = enrollmentRepository.findEnrollments(query);
      if (enrollments == null || enrollments.isEmpty()) {
        return checkInData;
      }
    }

    Enrollment enrollment = enrollments.get(0);

    if (enrollment == null) {
      return checkInData;
    }

    return getCheckInData(checkInData, enrollment, true);
  }

  @Override
  public PracticeCheckIn getPracticeCheckInData(Long clinicId, Long patientId) {
    Optional<PracticeCheckIn> checkin =
        practiceCheckInRepository.findFirstByClinicIdAndPatientId(clinicId, patientId);
    if (checkin.isPresent()) {
      return checkin.get();
    } else {
      return null;
    }
  }

  @Override
  public PracticeCheckIn persistPracticeCheckInData(PracticeCheckIn practiceCheckIn) {
    IdentityContext identityContext = this.identityContextHolder.get();

    if (identityContext != null
        && identityContext.getPatientId() != null
        && identityContext.getClinicId() != null) {
      log.trace("using patient");
      practiceCheckIn.setClinicId(identityContext.getClinicId());
      practiceCheckIn.setPatientId(identityContext.getPatientId());
      practiceCheckIn.setCompletedBy(
          this.patientInfoService.getPatientName(
              identityContext.getClinicId(), identityContext.getPatientId()));
    } else {
      practiceCheckIn.setCompletedBy(HEALTH_TRACKER_NAME);
    }
    if (practiceCheckIn.getStatus() == PracticeCheckIn.Status.COMPLETED) {
      healthTrackerEventsPublisher.publishPracticeCheckinCompleted(practiceCheckIn);
    }
    return practiceCheckInRepository.save(practiceCheckIn);
  }

  @Override
  public List<CheckInResponse> getCheckInsByEnrollmentAndStatus(
      String enrollmentId, CheckInStatus checkInStatus, boolean includeClinicCollect) {
    Enrollment enrollment =
        enrollmentRepository
            .findById(enrollmentId)
            .orElseThrow(
                () ->
                    new RecordNotFoundException(
                        String.format("Enrollment with id %s not found", enrollmentId)));

    List<CheckIn> checkIns;
    if (checkInStatus == CheckInStatus.PENDING) {
      // inserts practice checkins or adds isGuided param to the pending checkins if
      // needed
      checkIns = buildPendingCheckins(enrollment);
    } else if (checkInStatus != null) {
      checkIns = checkInRepository.findByStatus(enrollmentId, checkInStatus);
    } else {
      checkIns = checkInRepository.findByEnrollmentId(enrollmentId);
    }

    // filter out CX-only check-ins
    if (!includeClinicCollect) {
      checkIns = checkIns.stream().filter(c -> !SurveyId.isClinicCollect(c.getSurveyId())).toList();
    }

    // remove when HT-5236_checkin_schedule_collection is live
    // Once checkInSchedules are stored in the db we should use ht-server resolvers
    // to build the
    // schedule off of the checkin.scheduleId
    Map<String, CheckInSchedule> schedulesMap = buildSchedules(enrollment, checkIns);
    var checkInResponses =
        checkIns.stream()
            .map(
                c -> {
                  String scheduleId = c.getCheckInScheduleId();
                  if (scheduleId == null && c.getCheckInType() != null) {
                    scheduleId = c.getCheckInType().toString();
                  }

                  if (scheduleId != null) {
                    CheckInSchedule schedule = schedulesMap.get(scheduleId);
                    return new CheckInResponse(c, schedule);
                  } else {
                    return new CheckInResponse(c, null);
                  }
                })
            .toList();

    return checkInResponses;
  }

  private List<CheckIn> getMostRecentPendingCheckins(List<CheckIn> checkIns) {
    List<CheckIn> pendingCheckIns = new ArrayList<>();
    List<CheckIn> allPending =
        checkIns.stream()
            .filter(c -> c.getStatus() == CheckInStatus.PENDING)
            .filter(c -> c.getCheckInType() != null)
            .sorted(Comparator.comparing(CheckIn::getScheduleDate).reversed())
            .toList();

    LocalDate lastPendingDate = null;
    for (CheckIn ci : allPending) {
      if (lastPendingDate == null) {
        lastPendingDate = ci.getScheduleDate();
      }
      if (lastPendingDate.isAfter(ci.getScheduleDate())) {
        break;
      }
      pendingCheckIns.add(ci);
    }
    return pendingCheckIns;
  }

  private CheckInData getCheckInData(
      CheckInData checkInData, Enrollment enrollment, boolean includePatientInfo) {

    if (enrollment == null) {
      return checkInData;
    }

    String enrollmentId = enrollment.getId();

    // load check-ins for enrollment *once* to determine scheduling dates
    List<CheckIn> checkins =
        checkInRepository.findByEnrollmentIdOrderByScheduleDateDesc(enrollmentId);

    // need to determine checkin type to set survey for program
    // or override if combo and today is only oral or symptom
    CheckInType checkInType = null;
    List<CheckIn> pendingList = new ArrayList<>();
    for (CheckIn checkIn : getMostRecentPendingCheckins(checkins)) {
      CheckInType type = checkIn.getCheckInType();
      if (checkInType == null) {
        checkInType = type;
      } else if (checkInType != type) { // must be combo
        checkInType = CheckInType.COMBO;
      }

      pendingList.add(checkIn);
    }

    checkInData.setPending(pendingList);
    List<CheckIn> missedOrals = getMissedCheckinsForBackfill(enrollmentId, 6, CheckInType.ORAL);
    boolean hasMissedOral = missedOrals != null && missedOrals.size() > 0;

    // current rule is only the most recent six since last completed (ORAL only)
    // TODO: pull from config (GNE will be different)
    checkInData.setMissed(missedOrals);

    // if we have a combo survey id, adjust if we only have oral or symptom on this
    // day
    // note this logic will be replaced by the program logic below
    // HT CX
    if (SurveyId.ORAL_ADHERENCE_HT_CX.equalsIgnoreCase(enrollment.getSurveyId())) {
      if (checkInType == CheckInType.ORAL || (pendingList.isEmpty() && hasMissedOral))
        enrollment.setSurveyId(SurveyId.ORAL_ADHERENCE_CX);

      if (checkInType == CheckInType.SYMPTOM) enrollment.setSurveyId(SurveyId.HEALTH_TRACKER_CX);
    }

    // ORAL HT PX
    if (SurveyId.ORAL_ADHERENCE_HT_PX.equalsIgnoreCase(enrollment.getSurveyId())) {
      if (checkInType == CheckInType.ORAL || (pendingList.isEmpty() && hasMissedOral))
        enrollment.setSurveyId(SurveyId.ORAL_ADHERENCE_PX);

      if (checkInType == CheckInType.SYMPTOM) enrollment.setSurveyId(SurveyId.HEALTH_TRACKER_PX);
    }

    // if we have program id, get program config, and get surveyId
    String surveyId =
        surveyConfigService.getSurveyIdForProgram(
            checkInType, enrollment, pendingList.isEmpty() && hasMissedOral);
    if (StringUtils.isNotBlank(surveyId)) {
      log.debug("setting survey id to {}", surveyId);
      enrollment.setSurveyId(surveyId);
    }

    checkInData.setMissed(missedOrals);
    // TODO: implement following methods using in-memory check-ins (no need to go back to the db for
    // these answers)
    checkInData.setCompletedCount(checkInRepository.getCompletedCount(enrollmentId));
    checkInData.setTotalCount(checkInRepository.getTotalCount(enrollmentId));

    int adherencePercent = Math.round(checkInRepository.getAdherencePercent(enrollmentId));
    checkInData.setAdherencePercent(adherencePercent);

    checkInData.setEnrollment(enrollment);
    if (includePatientInfo) {
      checkInData.setUser(getPatientInfo(enrollment.getClinicId(), enrollment.getPatientId()));
    }

    LocalDateTime nextCheckInDate =
        CheckInDates.forEnrollment(enrollment, checkins).getNextCheckInDate();
    if (nextCheckInDate != null) {
      CheckIn nextCheckIn = new CheckIn();
      nextCheckIn.setScheduleDate(nextCheckInDate.toLocalDate());
      nextCheckIn.setScheduleTime(nextCheckInDate.toLocalTime());
      checkInData.setNext(nextCheckIn);
      checkInData.setNextCheckIn(
          LocalDateTime.of(nextCheckIn.getScheduleDate(), nextCheckIn.getScheduleTime()));
    }

    populateCheckInDates(checkInData, enrollment, checkins);

    // TODO remov
    // set medication taken flag
    checkInData
        .getPending()
        .forEach(
            pending -> {
              boolean medicationTaken =
                  checkins.stream()
                      .filter(c -> c.getStatus() == CheckInStatus.COMPLETED)
                      .filter(c -> c.getCheckInType() == pending.getCheckInType())
                      .anyMatch(c -> c.getMedicationTaken() == Boolean.TRUE);
              pending.setMedicationTaken(medicationTaken);
            });

    checkInData.setIsProCtcaeFormat(proFormatManager.followsCtcaeStandard(enrollment));

    return checkInData;
  }

  private void populateCheckInDates(
      CheckInData checkInData, Enrollment enrollment, List<CheckIn> checkins) {
    var oralCheckInSummary = CheckInDates.forCheckInType(enrollment, CheckInType.ORAL, checkins);
    checkInData.setLastOralCheckIn(oralCheckInSummary.getLastCheckInDate());
    checkInData.setNextOralCheckIn(oralCheckInSummary.getNextCheckInDate());

    var symptomCheckInSummary =
        CheckInDates.forCheckInType(enrollment, CheckInType.SYMPTOM, checkins);
    checkInData.setNextSymptomCheckIn(symptomCheckInSummary.getNextCheckInDate());
    checkInData.setLastSymptomCheckIn(symptomCheckInSummary.getLastCheckInDate());
  }

  private void setCheckInFields(
      Enrollment enrollment, CheckIn baseCheckIn, SurveyItemPayload surveyItemPayload) {
    baseCheckIn.setDeclineACall(declineACall(surveyItemPayload));
    baseCheckIn.setDeclineACallComment(declineACallComment(surveyItemPayload));
    baseCheckIn.setStatus(CheckInStatus.COMPLETED);
    baseCheckIn.setSurveyPayload(surveyItemPayload);

    IdentityContext identityContext = this.identityContextHolder.get();
    if (identityContext != null) {
      if (identityContext.getClinicianName() != null
          && !HEALTH_TRACKER_NAME.equalsIgnoreCase(identityContext.getClinicianName())) {
        log.trace("using clinician name {} for completedBy", identityContext.getClinicianName());
        baseCheckIn.setCompletedBy(identityContext.getClinicianName());
      } else if (identityContext.getPatientId() != null && identityContext.getClinicId() != null) {
        log.trace("using patient");
        baseCheckIn.setCompletedBy(
            this.patientInfoService.getPatientName(
                identityContext.getClinicId(), identityContext.getPatientId()));
      } else {
        log.trace("using clinician name {} for completedBy", identityContext.getClinicianName());
        baseCheckIn.setCompletedBy(identityContext.getClinicianName());
      }
    } else {
      baseCheckIn.setCompletedBy(HEALTH_TRACKER_NAME);
    }

    ClinicConfig clinicConfig = surveyConfigService.getClinicConfig(enrollment.getClinicId());
    if (baseCheckIn.getCheckInType() == CheckInType.ORAL) {
      baseCheckIn.setMedicationTaken(medicationTaken(surveyItemPayload));
      baseCheckIn.setMedicationStarted(getMedicationStarted(surveyItemPayload));
      baseCheckIn.setPatientReportedTxStartDate(
          getPatientReportedTxStartDate(enrollment, surveyItemPayload, clinicConfig));
      baseCheckIn.setTxStartDate(enrollment.getTxStartDate());
      baseCheckIn.setEnrollmentPatientReportedStartDate(enrollment.getPatientReportedTxStartDate());
      baseCheckIn.setEnrollmentReminderStartDate(enrollment.getReminderStartDate());
    }
  }

  private CheckIn backfillCheckIn(
      Enrollment enrollment, SurveyItemPayload surveyItemPayload, CheckIn baseCheckIn) {
    setCheckInFields(enrollment, baseCheckIn, surveyItemPayload);
    baseCheckIn.setEnrollmentId(enrollment.getId());
    baseCheckIn.setCheckInType(CheckInType.ORAL);
    baseCheckIn.setLocationId(enrollment.getLocationId());
    baseCheckIn.setClinicId(enrollment.getClinicId());
    baseCheckIn.setPatientId(enrollment.getPatientId());
    baseCheckIn.setScheduleDate(
        getLocalDate(
            surveyItemPayload.getPayload().get("medicationTakenDate").toString(), enrollment));

    baseCheckIn.setScheduleTime(
        LocalTime.parse(enrollment.getReminderTime(), DateTimeFormatter.ofPattern("H:mm")));

    CheckIn createdCheckIn = this.checkInBackfill(baseCheckIn);
    baseCheckIn.setId(createdCheckIn.getId());
    log.debug(
        "Created backfill check-in with id {} for medicationStarted payload with no id",
        createdCheckIn.getId());
    surveyItemPayload.setId(createdCheckIn.getId());

    return createdCheckIn;
  }

  private CheckIn submitCheckIn(
      Enrollment enrollment, SurveyItemPayload surveyItemPayload, CheckIn baseCheckIn) {
    setCheckInFields(enrollment, baseCheckIn, surveyItemPayload);
    baseCheckIn.setId(surveyItemPayload.getId());
    log.debug("saving check in: {}", baseCheckIn);

    return checkInRepository.save(baseCheckIn);
  }

  private CheckIn getBaseCheckIn(SurveyItemPayload targetPayloadItem, SurveyPayload surveyPayload) {
    log.debug(
        "getting baseCheckIn based on {} item and {} payload", targetPayloadItem, surveyPayload);
    Optional<CheckIn> optionalBaseCheckIn =
        targetPayloadItem.hasNullOrBlankId()
            ? Optional.empty()
            : checkInRepository.findById(targetPayloadItem.getId());

    if (optionalBaseCheckIn.isEmpty() && !targetPayloadItem.hasNullOrBlankId()) {
      var errMessage =
          MessageFormatter.format(
              "Checkin {} does not exist and is not backfill", targetPayloadItem);

      log.error(errMessage.getMessage());
      throw new BadDataException(errMessage.getMessage());
    }

    CheckIn baseCheckIn =
        optionalBaseCheckIn.isPresent()
            ? optionalBaseCheckIn.get()
            : new CheckIn(surveyPayload.getEnrollmentId());

    return baseCheckIn;
  }

  private Enrollment processSurveyItemPayloads(
      SurveyPayload surveyPayload,
      List<SurveyItemPayload> surveyItemPayloads,
      List<CheckIn> checkins) {
    if (surveyItemPayloads == null || surveyItemPayloads.isEmpty()) {
      return null;
    }

    Predicate<SurveyItemPayload> noIdCheck = si -> si.hasNullOrBlankId();
    Predicate<SurveyItemPayload> medsStartingCheck =
        si -> si.getPayload() != null && "yes".equals(si.getPayload().get("medicationStarted"));
    Predicate<SurveyItemPayload> isInvalid = noIdCheck.and(medsStartingCheck.negate());

    if (surveyItemPayloads.stream().anyMatch(isInvalid)) {
      log.warn(
          "underdefined surveyItems will be dropped in {} from  {}",
          surveyItemPayloads,
          surveyPayload);
      surveyItemPayloads.removeIf(isInvalid);
    }

    // ensure payloads without id are last so we can use other payloads info when we
    // backfill
    surveyItemPayloads.sort(
        (SurveyItemPayload a, SurveyItemPayload b) -> {
          if (a.hasNullOrBlankId() && !b.hasNullOrBlankId()) {
            return 1;
          } else if (!a.hasNullOrBlankId() && b.hasNullOrBlankId()) {
            return -1;
          } else {
            return 0;
          }
        });

    if (surveyItemPayloads.size() < 1) {
      var errMessage =
          MessageFormatter.format(
              "No surveyItemPayloads present.  SurveyPayload: {}", surveyPayload);

      log.error(errMessage.getMessage());
      throw new BadDataException(errMessage.getMessage());
    }

    log.debug("sorted surveyItemPayloads = {}", surveyItemPayloads);
    String checkInEnrollmentId =
        getBaseCheckIn(surveyItemPayloads.get(0), surveyPayload).getEnrollmentId();
    Optional<Enrollment> optionalEnrollment = enrollmentRepository.findById(checkInEnrollmentId);
    if (optionalEnrollment.isEmpty()) {
      var errMessage =
          MessageFormatter.format("Insufficient information in survey payload: {}", surveyPayload);

      log.error(errMessage.getMessage());
      throw new BadDataException(errMessage.getMessage());
    }

    Enrollment enrollment = optionalEnrollment.get();

    for (SurveyItemPayload surveyItemPayload : surveyItemPayloads) {
      // set a legacy field medicationStartedDate from the current medicationTakenDate
      CheckIn currentCheckIn = getBaseCheckIn(surveyItemPayload, surveyPayload);
      setMedicationStartedDate(surveyItemPayload);
      currentCheckIn =
          noIdCheck.and(medsStartingCheck).test(surveyItemPayload)
              ? backfillCheckIn(enrollment, surveyItemPayload, currentCheckIn)
              : submitCheckIn(enrollment, surveyItemPayload, currentCheckIn);

      checkins.add(currentCheckIn);
    }

    return enrollment;
  }

  // called for oral checkins only
  private void rescheduleIfMedsDateChange(List<CheckIn> checkins, Enrollment enrollment) {
    if (enrollment == null) {
      return;
    }
    boolean needToReschedule = false;
    SurveyItemPayload dateChangeSurveyItemPayload = null; // getting set if needToReschedule
    ClinicConfig clinicConfig = surveyConfigService.getClinicConfig(enrollment.getClinicId());

    for (CheckIn checkin : checkins) {
      // check if medication start date has been changed
      if (changeScheduleIfRequested(enrollment, checkin.getSurveyPayload(), clinicConfig)) {
        needToReschedule = true;
        dateChangeSurveyItemPayload = checkin.getSurveyPayload();
      }
    }

    // Make rescehdule call only after all changes to existing checkins are done to
    // avoid concurrent
    // DB updates
    if (needToReschedule) {
      LocalDate submittedStartDate =
          getPatientReportedTxStartDate(enrollment, dateChangeSurveyItemPayload, clinicConfig);
      log.info(
          "Patient reported start date {} is different from enrollment. Re-scheduling...",
          submittedStartDate);
      schedulingService.schedule(enrollment, false);
    }
  }

  private void setMedicationStartedDate(SurveyItemPayload payload) {
    String medicationTakenDateStr =
        (String) payload.getPayload().get(CheckInService.MEDICATION_TAKEN_DATE);
    if (!Strings.isNullOrEmpty(medicationTakenDateStr)) {
      payload
          .getPayload()
          .put(CheckInService.MEDICATION_STARTED_DATE_QUESTIONS_ID, medicationTakenDateStr);
    }
  }

  private LocalDate getPatientReportedTxStartDate(
      Enrollment e, SurveyItemPayload payload, ClinicConfig clinicConfig) {
    if (clinicConfig == null
        || (clinicConfig != null && !clinicConfig.isFeatureEnabled("patient-reported-start-date")))
      return null;

    // completedCount check will make sure that the user has not completed and check
    // ins prior to
    // this one
    int completedCount = Math.round(checkInRepository.getCompletedCount(e.getId()));
    if (e.getFirstCheckInResponseDate() == null && completedCount == 0) {
      e.setFirstCheckInResponseDate(LocalDate.now());
      enrollmentRepository.save(e);
    }

    String medicationStartedDateStr =
        (String) payload.getPayload().get(CheckInService.MEDICATION_STARTED_DATE_QUESTIONS_ID);
    String medicationTakenDateStr =
        (String) payload.getPayload().get(CheckInService.MEDICATION_TAKEN_DATE);

    LocalDate submittedStartDate = null;
    if (!Strings.isNullOrEmpty(medicationStartedDateStr)) {
      submittedStartDate = getLocalDate(medicationStartedDateStr, e);
    } else if (!Strings.isNullOrEmpty(medicationTakenDateStr)) {
      submittedStartDate = getLocalDate(medicationTakenDateStr, e);
    }

    if (submittedStartDate != null) {
      e.setPatientReportedTxStartDate(submittedStartDate);
      e.setReminderStartDate(submittedStartDate);
      enrollmentRepository.save(e);
    }

    return submittedStartDate;
  }

  private boolean changeScheduleIfRequested(
      Enrollment e, SurveyItemPayload surveyItemPayload, ClinicConfig clinicConfig) {
    boolean needToReschedule = false;

    LocalDate submittedStartDate =
        getPatientReportedTxStartDate(e, surveyItemPayload, clinicConfig);

    if (submittedStartDate != null) {
      for (CheckInSchedule schedule : e.getSchedules()) {
        // detect if start date entered in survey mismatch the schedule in DB
        if (!submittedStartDate.atStartOfDay().equals(schedule.getStartDate().atStartOfDay())) {
          needToReschedule = true;
          schedule.setStartDate(submittedStartDate);
          schedule.setEndDate(null);
          log.debug("need to reschedule {}", schedule);
        }
      }
    }

    return needToReschedule;
  }

  /**
   * Track question about medication taking. It differentiates the behaviour of patient expierence
   *
   * @param surveyItemPayload
   * @return Boolean
   */
  private boolean medicationTaken(SurveyItemPayload surveyItemPayload) {
    if (surveyItemPayload != null && surveyItemPayload.getPayload() != null) {
      if (surveyItemPayload.getPayload().containsKey(MEDICATION_TAKEN_QUESTION_ID)
          && surveyItemPayload
              .getPayload()
              .get(MEDICATION_TAKEN_QUESTION_ID)
              .equals(MEDICATION_TAKEN_ANSWER_ID)) {
        return true;
      }
      if (surveyItemPayload.getPayload().containsKey(MEDICATION_STARTED_QUESTION_ID)
          && surveyItemPayload
              .getPayload()
              .get(MEDICATION_STARTED_QUESTION_ID)
              .equals(MEDICATION_TAKEN_ANSWER_ID)) {
        return true;
      }
    }

    return false;
  }

  private boolean declineACall(SurveyItemPayload surveyItemPayload) {
    return surveyItemPayload != null ? surveyItemPayload.isDeclineACall() : false;
  }

  private String declineACallComment(SurveyItemPayload surveyItemPayload) {
    return surveyItemPayload != null ? surveyItemPayload.getDeclineACallComment() : null;
  }

  private Boolean getMedicationStarted(SurveyItemPayload surveyItemPayload) {
    if (!surveyItemPayload.getPayload().containsKey(MEDICATION_STARTED_QUESTION_ID)) return null;

    return surveyItemPayload
        .getPayload()
        .get(MEDICATION_STARTED_QUESTION_ID)
        .equals(MEDICATION_TAKEN_ANSWER_ID);
  }

  private PatientInfo getPatientInfo(Long clinicId, Long patientId) {
    List<PatientInfo> patients = patientInfoClient.getApi().getPatients(clinicId, patientId);
    return patients != null && !patients.isEmpty() ? patients.get(0) : null;
  }

  private LocalDate getLocalDate(String medicationStartedDateStr, Enrollment enrollment) {
    // should be string of format YYYY-MM-DD but could be timestamp
    LocalDate result = null;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    try {
      result = LocalDate.parse(medicationStartedDateStr, formatter);
    } catch (DateTimeParseException dtp) {
      Long timestamp = Long.parseLong(medicationStartedDateStr);
      result =
          DateTimeUtils.toLocalDateTime(new Date(timestamp), enrollment.getReminderTimeZone())
              .toLocalDate();
    }

    return result;
  }

  /**
   * finds the last missed checkins of the specified type since the last completed
   *
   * @param enrollmentId
   * @param limit
   * @param checkInType
   * @return returns up to the specified limit
   */
  private List<CheckIn> getMissedCheckinsForBackfill(
      String enrollmentId, int limit, CheckInType checkInType) {
    // find last completed oral checkin
    Stream<CheckIn> checkIns =
        this.checkInRepository.findTopByEnrollmentIdAndCheckInTypeAndStatusOrderByScheduleDateDesc(
            enrollmentId, checkInType, CheckInStatus.COMPLETED);
    CheckIn lastCompleted = checkIns.findFirst().orElse(null);
    // get valid date for search, using EPOCH (1970-01-01) as min is
    // (-999999999-01-01), which fails
    // conversion to Date in mongo
    LocalDate lastCompletedDate =
        lastCompleted == null ? LocalDate.EPOCH : lastCompleted.getScheduleDate();
    // find the last six missed since last completed
    Stream<CheckIn> missedCheckIns =
        this.checkInRepository
            .findByEnrollmentIdAndCheckInTypeAndStatusAndScheduleDateGreaterThanOrderByScheduleDateDesc(
                enrollmentId, checkInType, CheckInStatus.MISSED, lastCompletedDate);

    return missedCheckIns.limit(limit).collect(Collectors.toList());
  }

  private Map<String, CheckInSchedule> buildSchedules(
      Enrollment enrollment, List<CheckIn> checkIns) {
    var schedules = new HashMap<String, CheckInSchedule>();
    enrollment
        .getSchedules()
        .forEach(
            s -> {
              String scheduleId = s.getId() != null ? s.getId() : s.getCheckInType().toString();
              s.setCurrentCycleStartDate(enrollment.getCurrentCycleStartDate());
              if (enrollment.getCurrentCycleNumber() != null) {
                s.setCurrentCycleNumber(enrollment.getCurrentCycleNumber().intValue());
              }
              s.setStatus(CheckInScheduleStatus.fromEnrollmentStatus(enrollment.getStatus()));
              schedules.put(scheduleId, s);
            });
    return schedules;
  }

  private List<CheckIn> buildPendingCheckins(Enrollment enrollment) {
    boolean hasDonePracticeCheckIn =
        practiceCheckInRepository
            .findFirstByClinicIdAndPatientId(enrollment.getClinicId(), enrollment.getPatientId())
            .isPresent();

    List<CheckIn> pendingCheckIns =
        checkInRepository.findByStatus(enrollment.getId(), CheckInStatus.PENDING);
    final boolean hasCompletedACheckIn =
        checkInRepository.hasCompletedACheckIn(enrollment.getPatientId());
    if (!hasDonePracticeCheckIn && !hasCompletedACheckIn && pendingCheckIns.isEmpty()) {
      List<CheckInSchedule> schedules = enrollment.getSchedules();
      // TODO: handle non oral/symptom surveys. Do we create practice checkIns for them?
      List<CheckIn> practiceCheckIns =
          schedules.stream()
              .filter(s -> s.getStatus() == CheckInScheduleStatus.ACTIVE)
              .map(s -> checkInCreationService.buildPracticeCheckIn(enrollment, s))
              .sorted()
              .toList();
      return practiceCheckIns;
    }

    if (pendingCheckIns.isEmpty()) {
      return pendingCheckIns;
    }

    boolean hasOralCheckIn =
        pendingCheckIns.stream().anyMatch(pc -> SurveyId.isOralSurvey(pc.getSurveyId()));

    for (var pendingCheckIn : pendingCheckIns) {
      if (!hasOralCheckIn
          && (SurveyId.isOralSurvey(pendingCheckIn.getSurveyId())
              || SurveyId.isSymptomSurvey(pendingCheckIn.getSurveyId()))) {
        var missedCheckIns = getMissedCheckinsForBackfill(enrollment.getId(), 6, CheckInType.ORAL);
        pendingCheckIn.getCheckInParameters().put("missedCheckIns", missedCheckIns);
      }

      if (pendingCheckIn.getCreatedReason() == ReasonForCheckInCreation.SCHEDULED
          && !hasDonePracticeCheckIn
          && !hasCompletedACheckIn) {
        pendingCheckIn.getCheckInParameters().put("isGuided", true);
      }
    }

    return pendingCheckIns.stream().sorted().toList();
  }
}
