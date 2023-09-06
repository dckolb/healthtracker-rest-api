package com.navigatingcancer.healthtracker.api.data.model;

import lombok.*;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"type", "passed", "title", "note"})
public class CheckInResult {

	public enum ResultType {
		adherence, participation, symptom, action, participationRate, noAction, startDateUpdated, txStartDate
	}

	public CheckInResult(ResultType type, Boolean passed, String title, String note) {
		this.type = type;
		this.passed = passed;
		this.title = title;
		this.note = note;
	}

	public boolean isParticipation() {
		return type == ResultType.participation;
	}
	public boolean isNoAction() {
		return type == ResultType.noAction;
	}
	public boolean isSymptom() { return type == ResultType.symptom; }

	// FIXME why arent these private ?
	ResultType type;
	Boolean passed;
	String title;
	String note;

	// fields for GC integration
	String triageReasonType;
	String triageSymptomType;
	// NOTE : all updates need to go through setTriageSeverity() method below.
	@Setter(AccessLevel.NONE)
	String triageSeverity;

	private static final String TRIAGE_SEVERITY_MILD = "mild";
	private static final String TRIAGE_SEVERITY_MODERATE = "moderate";
	private static final String TRIAGE_SEVERITY_SEVERE = "severe";

	// FIXME : hacky solution to handle MN and non-MN clinics
	public void setTriageSeverity(boolean isProCtcae, String severityCode, String triageSeverity) {
		if(isProCtcae) {
		    switch (severityCode) {
				case "1":
					this.triageSeverity = TRIAGE_SEVERITY_MILD;
					break;
				case "2":
					this.triageSeverity = TRIAGE_SEVERITY_MODERATE;
					break;
				case "3":
					this.triageSeverity = TRIAGE_SEVERITY_SEVERE;
					break;
				case "4":
					this.triageSeverity = TRIAGE_SEVERITY_SEVERE;
					break;
				default:
					this.triageSeverity = TRIAGE_SEVERITY_SEVERE;
			}

		} else {
			switch (triageSeverity.toLowerCase()) {
				case TRIAGE_SEVERITY_MILD:
					this.triageSeverity = TRIAGE_SEVERITY_MILD;
					break;
				case TRIAGE_SEVERITY_MODERATE:
					this.triageSeverity = TRIAGE_SEVERITY_MODERATE;
					break;
				case TRIAGE_SEVERITY_SEVERE:
					this.triageSeverity = TRIAGE_SEVERITY_SEVERE;
					break;
				case "VERY SEVERE":
					this.triageSeverity = TRIAGE_SEVERITY_SEVERE;
					break;
				default:
					this.triageSeverity = TRIAGE_SEVERITY_SEVERE;
            }
		}
	}
}
