package com.navigatingcancer.healthtracker.api.processor.model;

import com.navigatingcancer.healthtracker.api.data.model.Adherence;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInType;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.SideEffect;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.repo.CheckInRepository;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SurveyPayloadParser {

  @Autowired ProFormatManager proFormatManager;

  @Autowired CheckInRepository checkInRepository;

  /**
   * Parse oral {@link Adherence} elements out of a {@link SurveyPayload}
   *
   * @param enr
   * @param sp
   * @return the list of parsed adherence elements
   */
  public List<Adherence> parseOralAdherence(Enrollment enr, SurveyPayload sp) {
    if (sp == null || sp.getOral() == null) {
      return Collections.emptyList();
    }

    List<String> checkInIds =
        sp.getOral().stream().map(s -> s.getId()).collect(Collectors.toList());

    // FIXME: handle survey instance (dont use check in type)
    CheckIn lastCheckin = checkInRepository.getLastCheckinByType(checkInIds, CheckInType.ORAL);
    if (lastCheckin != null) {
      String svId = lastCheckin.getId();
      List<SurveyItemPayload> lastOral =
          sp.getOral().stream().filter(s -> svId.equals(s.getId())).collect(Collectors.toList());
      if (lastOral.isEmpty()) {
        log.warn("list of oral survey does not contain oral survey", sp);
      } else {
        AdherenceParser adherenceParser = new AdherenceParser();
        return adherenceParser.parse(lastOral, enr.getMedication());
      }
    }

    return Collections.emptyList();
  }

  /**
   * Parse {@link SideEffect} elements out of a {@link SurveyPayload}
   *
   * @param enr
   * @param sp
   * @return the list of parsed side effect elements
   */
  public List<SideEffect> parseSideEffects(Enrollment enr, SurveyPayload sp) {
    if (sp == null || sp.getSymptoms() == null) {
      return Collections.emptyList();
    }

    boolean isProCtcaeFormat = proFormatManager.followsCtcaeStandard(enr);

    List<SideEffect> symptoms =
        null; // if symptoms persent something must be reported, even if it is no symptoms
    // find the very last symptoms checkin and survey responses
    List<String> checkInIds =
        sp.getSymptoms().stream().map(s -> s.getId()).collect(Collectors.toList());

    // FIXME: HT-5295 handle survey instance (dont use check in type)
    CheckIn lastCheckin = checkInRepository.getLastCheckinByType(checkInIds, CheckInType.SYMPTOM);
    if (lastCheckin == null) {
      if (!checkInIds.isEmpty()) {
        log.warn("failed to find checkin matching symptom survey", sp);
      }
    } else {
      String svId = lastCheckin.getId();
      List<SurveyItemPayload> lastSymptoms =
          sp.getSymptoms().stream()
              .filter(s -> svId.equals(s.getId()))
              .collect(Collectors.toList());
      if (lastSymptoms.isEmpty()) {
        log.warn("list of symptom survey does not contain symptoms", sp);
      } else {
        symptoms = SymptomParser.parseIntoSideEffects(isProCtcaeFormat, lastSymptoms);
      }
    }
    if (symptoms == null || symptoms.isEmpty()) {
      // Symptoms list was empty or we failed to find matching check-in or failed to parse symptoms
      // TODO. If we failed to parse symptoms we probably should not be reporting no symptoms?
      SideEffect se = new SideEffect();
      se.setSymptomType("No side effects");
      se.setSeverity("");
      symptoms = List.of(se);
    }
    return symptoms;
  }
}
