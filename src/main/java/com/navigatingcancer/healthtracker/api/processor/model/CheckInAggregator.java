package com.navigatingcancer.healthtracker.api.processor.model;

import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.CheckInResult;
import com.navigatingcancer.healthtracker.api.data.model.CheckInStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.service.CheckInService;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor
public class CheckInAggregator {

    @NonNull
    private List<CheckIn> checkIns;

    @Getter
    @NonNull
    private HealthTrackerStatus htStatus;

    @NonNull
    private Boolean isProCtcaeFormat;

    private static final Set<String> nonLowSeverity = new HashSet<>(Arrays.asList(new String[]{"2","3","4","5","6","7","8","9","10"}));

    public boolean patientHasNotStartedMedication() {
        return count(0, CheckIn::getHasNotStartedMedication) > 0;
    }

    public boolean patientReportedDifferentStartDate() {
        return count(0, CheckIn::patientReportedDifferentStartDate) > 0;
    }

    public boolean patientHasYetToReportStartDate() {
        return count(0, CheckIn::patientHasYetToReportStartDate) > 0;
    }

    public int countAnswerForMandatoryProCtcaeSymptoms(int duration, String answerIds) {
        if(duration < 0 || answerIds.isEmpty())
            return 0;

        return count(duration, checkIn -> answerForMandatoryProCtcaeQuestionContains(checkIn, splitToSet(answerIds)));
    }

    public int countAnswersWithHighSeveritys(int duration) {
        return count(duration, checkIn -> answerIn(checkIn, nonLowSeverity));
    }

    public int countAnswerForNonMandatoryProCtcaeSymptoms(int duration,  String symptomPart, String answerIds) {
        if(duration < 0 || answerIds.isEmpty())
            return 0;

        return count(duration, checkIn -> answerForNonMandatoryProCtcaeQuestionContains(checkIn, symptomPart,splitToSet(answerIds)));
    }

    public String countAnswerMessage(int duration, String questionId, String answerId) {
        int count = countAnswer(duration, questionId, answerId);
        return String.format("Missed %d out of %d recent doses", count, duration);
    }

    public Integer countAnswer(int duration, String questionId, String answerId) {
        if(duration < 0 || questionId.isEmpty() || answerId.isEmpty())
            return 0;

        return count(duration, checkIn -> questionMatchAndAnswerMatch(checkIn, questionId, answerId));
    }

    public boolean anyCompleted() {
        boolean res = false;
        if( checkIns != null && checkIns.size() > 0 ) {
            res = checkIns.stream().anyMatch(checkIn -> checkIn.getStatus() == CheckInStatus.COMPLETED);
        }
        return res;
    }

    public int countMissed(){
        LocalDate lastDateMissed = null;
        int countMissed = 0;
        for (CheckIn checkIn : checkIns) {
            // skip pending
            if (lastDateMissed == null &&
                    checkIn.getStatus() == CheckInStatus.PENDING &&
                    (checkIn.getScheduleDate().isAfter(LocalDate.now()) ||
                    checkIn.getScheduleDate().isEqual(LocalDate.now()))){
                continue;
            }
            if (checkIn.getStatus() != CheckInStatus.MISSED)
                break;
            // dedupe multiple checkIns in the same day
            if (lastDateMissed != null && lastDateMissed.isEqual(checkIn.getScheduleDate()))
                continue;
            lastDateMissed = checkIn.getScheduleDate();
            countMissed++;
        }
        return countMissed;
    }

    public Integer countMissed(int duration) {
        if(duration < 0)
            return 0;

        return count(duration, checkIn -> checkIn.getStatus() == CheckInStatus.MISSED);
    }

    public Integer countSideAffects(int duration, String questionPart, String answerIds) {
        if(duration < 0 || questionPart.isEmpty() || answerIds.isEmpty())
            return 0;

        return count(duration, checkIn -> questionContainsAndAnswerIn(checkIn, questionPart, splitToSet(answerIds)));
    }

    public Integer countSideAffects(int duration, String questionPart, String except, String answerIds) {
        return count(duration,
                checkIn -> questionContainsExceptAndAnswerIn(checkIn, questionPart, except, splitToSet(answerIds)));
    }

    public Integer countMNSevereSideAffects(int duration) {
        return count(duration, checkIn -> answerForMandatoryMNQuestionContains(checkIn, nonLowSeverity));
    }

    public Integer participationPercent(int durationDays) {
        if(durationDays < 0)
            return 0;

        return participationPercent(null, durationDays);
    }

    public Integer participationPercent(Integer rampUpPeriod, int durationDays) {
        Integer totalCompleted = count(durationDays, checkIn -> checkIn.getStatus() == CheckInStatus.COMPLETED);
        Integer total = count(durationDays, checkIn -> checkIn.getStatus() == CheckInStatus.MISSED || checkIn.getStatus() == CheckInStatus.COMPLETED);

        if (rampUpPeriod != null && total < rampUpPeriod) {
            return 100;
        }

        // assume participation percent is 100% if no missed or completed check-ins yet.
        return total == 0 ? 100 : (100 * totalCompleted / total);
    }

    // FIXME : duplicated computation in CustomCheckInRepository.getAdherencePercent()
    public Integer adherencePercent() {
        Integer medicationTakenCount = countAnswer(Integer.MAX_VALUE, CheckInService.MEDICATION_TAKEN_QUESTION_ID,
                CheckInService.MEDICATION_TAKEN_ANSWER_ID);
        Integer medicationNotTakenCount = countAnswer(Integer.MAX_VALUE, CheckInService.MEDICATION_TAKEN_QUESTION_ID,
                CheckInService.MEDICATION_NOT_TAKEN_ANSWER_ID);
        return medicationTakenCount == 0 ? 0
                : (100 * (medicationTakenCount) / (medicationTakenCount + medicationNotTakenCount));
    }

    public void addProCtcaeSymptom(int duration, String severities) {
        Set<String> severitiesList = splitToSet(severities);
        Map<String, Object> symptoms =
                collect(duration, e -> severitiesList.contains(e.getValue().toString()));

        addSymptom(duration, symptoms);
    }

    public void addSymptom(int duration, String symptomPart, String severities) {
        Set<String> severitiesList = splitToSet(severities);
        Map<String, Object> symptoms =
            collect(duration,
                    e -> e.getKey().contains(symptomPart)
                        && severitiesList.contains(e.getValue().toString()));
        addSymptom(duration, symptoms);
    }

    private void addSymptom(int duration, Map<String, Object> symptoms) {
        symptoms.forEach((symptom, severity) -> {
            String [] results = Symptom.parseSymptomStringFromPayload(symptom);
            String parsedSymptom = results[0];
            String fsilType = results[1];

            String description = SurveyDictionary.description(fsilType, isProCtcaeFormat, parsedSymptom, severity.toString());
            String title = SurveyDictionary.descriptiveTitle(fsilType, description, parsedSymptom);
            CheckInResult result =
                    new CheckInResult(CheckInResult.ResultType.symptom, false, title, buildNote(duration, parsedSymptom));

            htStatus.addResult(result);
            result.setTriageSymptomType(parsedSymptom);
            result.setTriageReasonType("symptom");
            result.setTriageSeverity(isProCtcaeFormat, severity.toString(), description);
        });
    }

    private String buildNote(Integer duration, String symptom) {
        StringBuilder note = new StringBuilder();

        // Not sure if this code is relevant since the pain related details will be under painComment
        collect(duration, e -> e.getKey().equals("painLocation") && e.getValue() != null)
                .forEach((String key, Object answer) -> {
                    note.append(String.format("Pain location: ", answer.toString())).append("\n");
                });

        populateAnswerFromDict(note, duration, "medicationNotTakenReason", "Reason: \"%s\"", new String[] {
                "I forgot to take it",
                "I don't think I am supposed to",
                "I am out of medication",
                "I am not feeling well",
                "My doctor paused my treatment", "Other"});

        populateAnswerFromDict(note, duration, symptom + "Interference", "It interferes with my daily activities %s.",
                new String[] { "a little bit", "somewhat", "quite a bit", "very much" });

        return note.toString();
    }

    private void populateAnswerFromDict(StringBuilder note, Integer duration, String questionName, String format,
                                        String[] dict) {
        collect(duration, e -> e.getKey().equals(questionName) && e.getValue() != null)
                .forEach((String key, Object answer) -> {
                    if (NumberUtils.isCreatable(answer.toString())) {
                        Integer i = Integer.parseInt(answer.toString()) - 1;
                        if (i > 0 && i < dict.length) {
                            note.append(String.format(format, dict[i])).append("\n");
                        }
                    }
                });
    }

    private Integer count(int duration, Predicate<CheckIn> p) {
        Set<LocalDate> daysAlreadyCounted = new HashSet<>();

        for (CheckIn checkIn : lastCheckIns(duration)) {
            if (p.test(checkIn) && !daysAlreadyCounted.contains(checkIn.getScheduleDate())) {
                daysAlreadyCounted.add(checkIn.getScheduleDate());
            }
        }

        return daysAlreadyCounted.size();
    }

    private List<CheckIn> lastCheckIns(int duration) {
        List<CheckIn> result = new ArrayList<>();
        for (CheckIn checkIn : checkIns) {

            if (Duration.between(checkIn.getScheduleDate().atStartOfDay(), lastCheckInDate().atStartOfDay())
                    .toDays() <= duration) {
                result.add(checkIn);
            } else {
                break;
            }
        }

        return result;
    }

    private Map<String, Object> collect(int duration, Predicate<Map.Entry<String, Object>> p) {
        Map<String, Object> result = new HashMap<>();
        for (CheckIn checkIn : lastCheckIns(duration)) {
            result.putAll(collect(checkIn, p));
        }
        return result;
    }

    private static Map<String, Object> collect(CheckIn checkIn, Predicate<Map.Entry<String, Object>> p) {
        Map<String, Object> result = new HashMap<>();
        if (checkIn.getSurveyPayload() != null && checkIn.getSurveyPayload().getPayload() != null) {
            for (Map.Entry<String, Object> e : checkIn.getSurveyPayload().getPayload().entrySet()) {
                if (p.test(e)) {
                    result.put(e.getKey(), e.getValue());
                }
            }
        }
        return result;
    }

    private LocalDate lastCheckInDate() {
        return checkIns == null || checkIns.isEmpty() ? null : checkIns.get(0).getScheduleDate();
    }

    private Set<String> splitToSet(String s) {
        return new HashSet<>(Arrays.asList(s.split(",")));
    }

    private static boolean questionMatchAndAnswerMatch(CheckIn checkIn, String questionId, String answerId) {
        return !collect(checkIn, e -> questionId.equals(e.getKey()) &&
                answerId.equals(e.getValue().toString())).isEmpty();

    }

    private static Set<String> PRO_CTCAE_MANDATORY_QUESTIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("painSeverity", "painFrequency", "painInterference",
                    "nauseaFrequency", "nauseaSeverity", "constipationSeverity")));

    private static boolean answerForMandatoryProCtcaeQuestionContains(CheckIn checkIn, Set<String> answerIds) {
        return !collect(checkIn, e -> PRO_CTCAE_MANDATORY_QUESTIONS.contains(e.getKey()) && answerIds.contains(e.getValue().toString()))
                .isEmpty();
    }

    private static boolean answerForMandatoryMNQuestionContains(CheckIn checkIn, Set<String> answerIds) {
        return !collect(checkIn, e -> PRO_CTCAE_MANDATORY_QUESTIONS.contains(e.getKey()) && answerIds.contains(e.getValue().toString()))
                .isEmpty();
    }

    private static boolean answerForNonMandatoryProCtcaeQuestionContains(CheckIn checkIn,  String symptomPart, Set<String> answerIds) {
        return !collect(checkIn, e -> !PRO_CTCAE_MANDATORY_QUESTIONS.contains(e.getKey()) &&
                e.getKey().contains(symptomPart) &&
                answerIds.contains(e.getValue().toString()))
                .isEmpty();
    }

    private static boolean questionContainsAndAnswerIn(CheckIn checkIn, String questionPart, Set<String> answerIds) {
        return !collect(checkIn, e -> e.getKey().contains(questionPart) && answerIds.contains(e.getValue().toString()))
                .isEmpty();
    }

    private static boolean answerIn(CheckIn checkIn,  Set<String> answerIds) {
        return !collect(checkIn, e -> answerIds.contains(e.getValue().toString()))
                .isEmpty();
    }

    private static boolean questionContainsExceptAndAnswerIn(CheckIn checkIn, String questionPart, String except,
                                                             Set<String> answerIds) {
        return !collect(checkIn, e -> e.getKey().contains(questionPart) && !e.getKey().contains(except)
                && answerIds.contains(e.getValue().toString())).isEmpty();
    }


}
