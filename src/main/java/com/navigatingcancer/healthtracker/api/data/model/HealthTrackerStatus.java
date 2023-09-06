package com.navigatingcancer.healthtracker.api.data.model;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyItemPayload;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.processor.model.SymptomDetails;
import com.navigatingcancer.patientinfo.domain.PatientInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;

@Data
@EqualsAndHashCode(callSuper = false)
@Document(collection = "ht_status")
public class HealthTrackerStatus extends AbstractDocument{

    private @NotNull Long clinicId;
    private @NotNull Long locationId;
    private HealthTrackerStatusCategory category; // triage, action_needed, watch_carefully etc
    private PatientInfo patientInfo;
    private String actionPerformed;
    private String actionPerformedBy;
    private Set<CheckInResult> ruleResults = new HashSet<>();
    private List<SymptomDetails> symptomDetails = new ArrayList<>();
    private List<String> ruleNames = new ArrayList<>();
    private LocalDate completedAt;
    private Integer adherencePercent;
    private String medication;
    public SurveyPayload surveyPayload;
    private Boolean followsProCtcaeFormat = false;
    private Integer missedCheckIns;
    private Boolean endCurrentCycle = false;
    private LocalDate nextScheduleDate;
    private Date lastPatientCheckInAt;
    private Instant lastMissedCheckInAt;
    private Date lastStatusChangedByClinicianAt;
    private Set<TherapyType> therapyTypes;
    private Set<String> lastCompletedCheckInIds; // Last completed or missed checkin IDs
    private Boolean declineACall;
    private String declineACallComment;
    private String proReviewId;

    public void clear() {
    	category = null;
    	ruleNames.clear();
        ruleResults.clear();
    }

    public void updateIfHigherPriorityCategory(HealthTrackerStatusCategory newCategory) {
        // Set category with highest priority
        // FIXME dont use implicit ordering in ENUM as priority
        if (this.category == null || this.category.ordinal() > newCategory.ordinal()) {
            setCategory(newCategory);
        }
    }

    public void addResult(CheckInResult.ResultType type, boolean passed, String desc, String note) {
        CheckInResult result = new CheckInResult(type, passed, desc, note);
        addResult(result);
    }

    // NOTE : There are strict rules for adding RuleResults so all addition should be done via the addResult method.
    public Set<CheckInResult> getRuleResults() {
        return Collections.unmodifiableSet(ruleResults);
    }

    public void addResult(CheckInResult result) {
        switch (result.getType()) {
            case participation:
                // only 1 rule allowed of this resultType
                ruleResults.removeIf(c -> c.isParticipation());
                ruleResults.add(result);
                break;
            case noAction:
                // add if
                //      1) rules are empty
                //      2) 1 rule present and rule is of resultType participation or noAction
                if(ruleResults.isEmpty() || (ruleResults.size() == 1 && ruleResults.stream().anyMatch(c -> c.isParticipation() || c.isNoAction()))) {
                    ruleResults.removeIf(c -> c.getType() == CheckInResult.ResultType.noAction);
                    ruleResults.add(result);
                }
                break;
            default:
                // remove noAction if any other result is added (besides participation)
                ruleResults.removeIf(c -> c.getType() == CheckInResult.ResultType.noAction);
                ruleResults.add(result);
        }
    }

    public void addResultAndSetTriageReason(CheckInResult.ResultType type, boolean passed, String desc, String note, String triageReasonType) {
        CheckInResult result = new CheckInResult(type, passed, desc, note);
        result.setTriageReasonType(triageReasonType);
        addResult(result);
    }

    public List<SurveyItemPayload> getSurveyPayloadSymptoms() {
        if(surveyPayload != null && surveyPayload.getContent() != null) {
            return surveyPayload.getSymptoms();
        } else {
            return null;
        }
    }

    public List<SurveyItemPayload> getSurveyPayloadOrals() {
        if(surveyPayload != null && surveyPayload.getContent() != null) {
            return surveyPayload.getContent().getOral();
        } else {
            return null;
        }
    }

}
