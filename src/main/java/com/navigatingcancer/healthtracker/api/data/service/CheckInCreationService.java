package com.navigatingcancer.healthtracker.api.data.service;

import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInSchedule;
import com.navigatingcancer.healthtracker.api.data.model.ContactPreferences;
import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyInstance;
import java.time.LocalDateTime;

public interface CheckInCreationService {

  CheckIn createCheckInForSchedule(
      Enrollment enrollment, CheckInSchedule schedule, LocalDateTime scheduledAt);

  CheckIn createCareTeamRequestedCheckIn(Enrollment enrollment, SurveyInstance surveyInstance);

  CheckIn createCareTeamRequestedCheckIn(
      Enrollment enrollment, SurveyInstance surveyInstance, ContactPreferences oneTimeContact);

  CheckIn createPatientRequestedCheckIn(Enrollment enrollment, SurveyInstance surveyInstance);

  CheckIn buildPracticeCheckIn(Enrollment enrollment, CheckInSchedule schedule);
}
