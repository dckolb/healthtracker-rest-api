package com.navigatingcancer.healthtracker.api.data.service.impl;

import static com.navigatingcancer.healthtracker.api.data.auth.AuthInterceptor.HEALTH_TRACKER_NAME;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.navigatingcancer.date.utils.DateTimeUtils;
import com.navigatingcancer.healthtracker.api.data.auth.IdentityContext;
import com.navigatingcancer.healthtracker.api.data.auth.IdentityContextHolder;
import com.navigatingcancer.healthtracker.api.data.model.*;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ClinicConfig;
import com.navigatingcancer.healthtracker.api.data.model.surveyConfig.ProgramConfig;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.CustomCheckInRepository;
import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.healthtracker.api.data.repo.PracticeCheckInRepository;
import com.navigatingcancer.healthtracker.api.data.service.CheckInService;
import com.navigatingcancer.healthtracker.api.data.service.EnrollmentService;
import com.navigatingcancer.healthtracker.api.data.service.PatientInfoService;
import com.navigatingcancer.healthtracker.api.data.service.SurveyConfigService;
import com.navigatingcancer.healthtracker.api.events.HealthTrackerEventsPublisher;
import com.navigatingcancer.healthtracker.api.metrics.HealthTrackerCounterMetric;
import com.navigatingcancer.healthtracker.api.metrics.MetersService;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.healthtracker.api.processor.model.ProFormatManager;
import com.navigatingcancer.healthtracker.api.rest.QueryParameters.EnrollmentQuery;
import com.navigatingcancer.healthtracker.api.rest.exception.BadDataException;
import com.navigatingcancer.patientinfo.PatientInfoClient;
import com.navigatingcancer.patientinfo.domain.PatientInfo;
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

  @Autowired private CustomCheckInRepository customCheckInRepository;

  @Autowired private EnrollmentRepository enrollmentRepository;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private SchedulingServiceImpl schedulingService;

  @Autowired private HealthTrackerStatusService healthTrackerStatusService;

  @Autowired private PatientInfoClient patientInfoClient;

  @Autowired private ProFormatManager proFormatManager;

  @Autowired private IdentityContextHolder identityContextHolder;

  @Autowired private PatientInfoService patientInfoService;

  @Autowired private SurveyConfigService surveyConfigService;

  @Autowired private HealthTrackerEventsPublisher healthTrackerEventsPublisher;

  @Autowired private MetersService metersService;

  private static final String BACKFILL_DATE = "medicationTakenDate";

  private Map<String, String> SURVEY_IDS =
      new ImmutableMap.Builder<String, String>()
          .put("ORAL_ADHERENCE_CX", CheckIn.ORAL_ADHERENCE_CX)
          .put("ORAL_ADHERENCE_PX", CheckIn.ORAL_ADHERENCE_PX)
          .put("ORAL_ADHERENCE_HT_CX", CheckIn.ORAL_ADHERENCE_HT_CX)
          .put("ORAL_ADHERENCE_HT_PX", CheckIn.ORAL_ADHERENCE_HT_PX)
          .put("HEALTH_TRACKER_CX", CheckIn.HEALTH_TRACKER_CX)
          .put("HEALTH_TRACKER_PX", CheckIn.HEALTH_TRACKER_PX)
          .put("PROCTCAE_CX", CheckIn.PROCTCAE_CX)
          .put("PROCTCAE_PX", CheckIn.PROCTCAE_PX)
          .build();

  /** Called when check-in survey (PX) is saved */
  @Override
  public void checkIn(SurveyPayload surveyPayload) {
    log.debug("checkIn with SurveyPayload {}", surveyPayload);

    // Persist ORAL check-in
    List<CheckIn> oralCheckins = new LinkedList<>();
    Enrollment oralEnrollment =
        processSurveyItemPayloads(surveyPayload, surveyPayload.getOral(), oralCheckins);
    // Persist symptom (side-affect) check-in
    List<CheckIn> checkins = new LinkedList<>(oralCheckins);
    Enrollment symptomEnrollment =
        processSurveyItemPayloads(surveyPayload, surveyPayload.getSymptoms(), checkins);
    // NOTE: both must be the same, legacy code expected that may not be the case, not sure why
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
    healthTrackerStatusService.push(enrollmentId, surveyPayload);
    // Send metric to DD
    Long clinicId = enrollment != null ? enrollment.getClinicId() : 0;
    metersService.incrementCounter(clinicId, HealthTrackerCounterMetric.CHECKIN_COMPLETED);
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
      // if this performs poorly we should update the queries to handle multiple enrollments at a
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

  /**
   * Ensure we're only looking at the most recent PENDING checkins
   *
   * @param enrollmentId
   * @return
   */
  private List<CheckIn> getMostRecentPendingCheckins(String enrollmentId) {
    List<CheckIn> pendingCheckIns = new ArrayList<>();
    List<CheckIn> allPending =
        checkInRepository.findByEnrollmentIdAndStatusOrderByScheduleDateDesc(
            enrollmentId, CheckInStatus.PENDING);
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

    checkInData.setPending(this.getMostRecentPendingCheckins(enrollmentId));
    // need to determine checkin type to set survey for program
    // or override if combo and today is only oral or symptom
    CheckInType checkInType = null;
    List<CheckIn> pendingList = new ArrayList<CheckIn>();
    for (CheckIn checkIn : checkInData.getPending()) {
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
    if (SURVEY_IDS.get("ORAL_ADHERENCE_HT_CX").equalsIgnoreCase(enrollment.getSurveyId())) {
      if (checkInType == CheckInType.ORAL || (pendingList.isEmpty() && hasMissedOral))
        enrollment.setSurveyId(SURVEY_IDS.get("ORAL_ADHERENCE_CX"));

      if (checkInType == CheckInType.SYMPTOM)
        enrollment.setSurveyId(SURVEY_IDS.get("HEALTH_TRACKER_CX"));
    }

    // ORAL HT PX
    if (SURVEY_IDS.get("ORAL_ADHERENCE_HT_PX").equalsIgnoreCase(enrollment.getSurveyId())) {
      if (checkInType == CheckInType.ORAL || (pendingList.isEmpty() && hasMissedOral))
        enrollment.setSurveyId(SURVEY_IDS.get("ORAL_ADHERENCE_PX"));

      if (checkInType == CheckInType.SYMPTOM)
        enrollment.setSurveyId(SURVEY_IDS.get("HEALTH_TRACKER_PX"));
    }

    // if we have program id, get program config, and get surveyId
    String surveyId =
        getSurveyIdForProgram(checkInType, enrollment, pendingList.isEmpty() && hasMissedOral);
    if (StringUtils.isNotBlank(surveyId)) {
      log.debug("setting survey id to {}", surveyId);
      enrollment.setSurveyId(surveyId);
    }

    checkInData.setMissed(missedOrals);
    checkInData.setCompletedCount(customCheckInRepository.getCompletedCount(enrollmentId));
    checkInData.setTotalCount(customCheckInRepository.getTotalCount(enrollmentId));

    int adherencePercent = Math.round(customCheckInRepository.getAdherencePercent(enrollmentId));
    checkInData.setAdherencePercent(adherencePercent);

    checkInData.setEnrollment(enrollment);
    if (includePatientInfo) {
      checkInData.setUser(getPatientInfo(enrollment.getClinicId(), enrollment.getPatientId()));
    }

    LocalDate nextCheckInDate = schedulingService.getNextCheckInDate(enrollment);
    if (nextCheckInDate != null) {
      CheckIn nextCheckIn = new CheckIn();
      nextCheckIn.setScheduleDate(nextCheckInDate);
      nextCheckIn.setScheduleTime(
          LocalTime.parse(enrollment.getReminderTime(), DateTimeFormatter.ofPattern("H:mm")));
      checkInData.setNext(nextCheckIn);
      checkInData.setNextCheckIn(
          LocalDateTime.of(nextCheckIn.getScheduleDate(), nextCheckIn.getScheduleTime()));
    }

    populateCheckInDates(checkInData, enrollment);

    // set medication taken flag
    checkInData
        .getPending()
        .forEach(
            pending -> {
              pending.setMedicationTaken(
                  checkInRepository.medicationTaken(enrollmentId, pending.getCheckInType()) > 0);
            });

    checkInData.setIsProCtcaeFormat(proFormatManager.followsCtcaeStandard(enrollment));

    return checkInData;
  }

  private String getSurveyIdForProgram(
      CheckInType checkInType, Enrollment enrollment, boolean emptyPendingAndMissedOral) {
    if (checkInType != null && StringUtils.isNotBlank(enrollment.getProgramId())) {
      ProgramConfig programConfig =
          this.surveyConfigService.getProgramConfig(enrollment.getProgramId());
      List<ProgramConfig.SurveyDef> surveyDefs = programConfig.getSurveys();
      for (ProgramConfig.SurveyDef surveyDef : surveyDefs) {
        if (checkInType.name().equalsIgnoreCase(surveyDef.getType())) {
          String key =
              enrollment.isManualCollect()
                  ? ProgramConfig.CLINIC_COLLECT
                  : ProgramConfig.PATIENT_COLLECT;

          String surveyId;
          if (checkInType.equals(CheckInType.COMBO) && emptyPendingAndMissedOral) {
            surveyId =
                enrollment.isManualCollect()
                    ? SURVEY_IDS.get("ORAL_ADHERENCE_CX")
                    : SURVEY_IDS.get("ORAL_ADHERENCE_PX");
          } else {
            surveyId = surveyDef.getSurveyIds().getOrDefault(key, null);
          }

          return surveyId;
        }
      }
    }
    return null;
  }

  private void populateCheckInDates(CheckInData checkInData, Enrollment enrollment) {

    LocalTime time =
        LocalTime.parse(enrollment.getReminderTime(), DateTimeFormatter.ofPattern("H:mm"));

    LocalDate nextOralCheckIn = schedulingService.getNextCheckInDate(enrollment, CheckInType.ORAL);
    if (nextOralCheckIn != null) {
      checkInData.setNextOralCheckIn(LocalDateTime.of(nextOralCheckIn, time));
    }

    LocalDate nextSymptomCheckIn =
        schedulingService.getNextCheckInDate(enrollment, CheckInType.SYMPTOM);
    if (nextSymptomCheckIn != null) {
      checkInData.setNextSymptomCheckIn(LocalDateTime.of(nextSymptomCheckIn, time));
    }

    String enrollmentId = enrollment.getId();

    CheckIn lastOralCheckIn =
        checkInRepository.findTopByEnrollmentIdAndCheckInTypeOrderByScheduleDateDesc(
            enrollmentId, CheckInType.ORAL);
    if (lastOralCheckIn != null) {
      checkInData.setLastOralCheckIn(
          LocalDateTime.of(lastOralCheckIn.getScheduleDate(), lastOralCheckIn.getScheduleTime()));
    }

    CheckIn lastSymptomCheckIn =
        checkInRepository.findTopByEnrollmentIdAndCheckInTypeOrderByScheduleDateDesc(
            enrollmentId, CheckInType.SYMPTOM);
    if (lastSymptomCheckIn != null) {
      checkInData.setLastSymptomCheckIn(
          LocalDateTime.of(
              lastSymptomCheckIn.getScheduleDate(), lastSymptomCheckIn.getScheduleTime()));
    }
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

    // ensure payloads without id are last so we can use other payloads info when we backfill
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

    // Make rescehdule call only after all changes to existing checkins are done to avoid concurrent
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

    // completedCount check will make sure that the user has not completed and check ins prior to
    // this one
    int completedCount = Math.round(customCheckInRepository.getCompletedCount(e.getId()));
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
    // get valid date for search, using EPOCH (1970-01-01) as min is (-999999999-01-01), which fails
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
}
