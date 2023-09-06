package com.navigatingcancer.healthtracker.api.data.model;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class HealthTrackerStatusTest {

    @Test
    public void updateIfHigherPriorityCategory_inAscOrder() {
        HealthTrackerStatus htStatus = new HealthTrackerStatus();

        assertTrue(htStatus.getCategory() == null);

        htStatus.updateIfHigherPriorityCategory(HealthTrackerStatusCategory.COMPLETED);
        assertEquals("Null category can be updated to COMPLETED", htStatus.getCategory(), HealthTrackerStatusCategory.COMPLETED);

        htStatus.updateIfHigherPriorityCategory(HealthTrackerStatusCategory.PENDING);
        assertEquals("COMPLETED category can be updated to PENDING", htStatus.getCategory(), HealthTrackerStatusCategory.PENDING);

        htStatus.updateIfHigherPriorityCategory(HealthTrackerStatusCategory.NO_ACTION_NEEDED);
        assertEquals("PENDING category can be updated to NO_ACTION_NEEDED", htStatus.getCategory(), HealthTrackerStatusCategory.NO_ACTION_NEEDED);

        htStatus.updateIfHigherPriorityCategory(HealthTrackerStatusCategory.WATCH_CAREFULLY);
        assertEquals("NO_ACTION_NEEDED category can be updated to WATCH_CAREFULLY", htStatus.getCategory(), HealthTrackerStatusCategory.WATCH_CAREFULLY);

        htStatus.updateIfHigherPriorityCategory(HealthTrackerStatusCategory.ACTION_NEEDED);
        assertEquals("WATCH_CAREFULLY category can be updated to ACTION_NEEDED", htStatus.getCategory(), HealthTrackerStatusCategory.ACTION_NEEDED);

        htStatus.updateIfHigherPriorityCategory(HealthTrackerStatusCategory.TRIAGE);
        assertEquals("ACTION_NEEDED category can be updated to TRIAGE", htStatus.getCategory(), HealthTrackerStatusCategory.TRIAGE);
    }

    @Test
    public void updateIfHigherPriorityCategory_cannotOverrideLowerPriority() {
        HealthTrackerStatus htStatus = new HealthTrackerStatus();
        htStatus.setCategory(HealthTrackerStatusCategory.TRIAGE);

        htStatus.updateIfHigherPriorityCategory(HealthTrackerStatusCategory.ACTION_NEEDED);
        assertEquals("TRIAGE category cannot be changed to ACTION_NEEDED", htStatus.getCategory(), HealthTrackerStatusCategory.TRIAGE);

        htStatus.updateIfHigherPriorityCategory(HealthTrackerStatusCategory.WATCH_CAREFULLY);
        assertEquals("TRIAGE category cannot be changed to WATCH_CAREFULLY", htStatus.getCategory(), HealthTrackerStatusCategory.TRIAGE);

        htStatus.updateIfHigherPriorityCategory(HealthTrackerStatusCategory.NO_ACTION_NEEDED);
        assertEquals("TRIAGE category cannot be changed to NO_ACTION_NEEDED", htStatus.getCategory(), HealthTrackerStatusCategory.TRIAGE);

        htStatus.updateIfHigherPriorityCategory(HealthTrackerStatusCategory.PENDING);
        assertEquals("TRIAGE category cannot be changed to PENDING", htStatus.getCategory(), HealthTrackerStatusCategory.TRIAGE);

        htStatus.updateIfHigherPriorityCategory(HealthTrackerStatusCategory.COMPLETED);
        assertEquals("TRIAGE category cannot be changed to COMPLETED", htStatus.getCategory(), HealthTrackerStatusCategory.TRIAGE);

    }

    @Test
    public void addResult_forParticipation() {
        HealthTrackerStatus htStatus = new HealthTrackerStatus();

        CheckInResult result = new CheckInResult();
        result.setType(CheckInResult.ResultType.participation);
        htStatus.addResult(result);

        assertTrue("Adds 1 participation result correctly", htStatus.getRuleResults().contains(result));

    }

    @Test
    public void addResult_forParticipationTwice() {
        HealthTrackerStatus htStatus = new HealthTrackerStatus();

        CheckInResult result = new CheckInResult();
        result.setTitle("P1");
        result.setType(CheckInResult.ResultType.participation);
        htStatus.addResult(result);

        CheckInResult result2 = new CheckInResult();
        result2.setTitle("P2");
        result2.setType(CheckInResult.ResultType.participation);
        htStatus.addResult(result2);

        assertFalse("deletes previous participation result", htStatus.getRuleResults().contains(result));
        assertTrue("replaces participation result with latest", htStatus.getRuleResults().contains(result2));
    }

    @Test
    public void addResult_forNoAction() {
        HealthTrackerStatus htStatus = new HealthTrackerStatus();

        CheckInResult result = new CheckInResult();
        result.setType(CheckInResult.ResultType.noAction);
        htStatus.addResult(result);

        assertTrue("Adds 1 noAction result correctly", htStatus.getRuleResults().contains(result));
    }

    @Test
    public void addResult_forNoAction_twice() {
        HealthTrackerStatus htStatus = new HealthTrackerStatus();

        CheckInResult result = new CheckInResult();
        result.setType(CheckInResult.ResultType.noAction);
        result.setTitle("no action 1");
        htStatus.addResult(result);

        CheckInResult result2 = new CheckInResult();
        result2.setType(CheckInResult.ResultType.noAction);
        result2.setTitle("no action 2");
        htStatus.addResult(result2);

        assertFalse("Deletes previous noAction", htStatus.getRuleResults().contains(result));
        assertTrue("Replaces with newer NoAction", htStatus.getRuleResults().contains(result2));
    }

    @Test
    public void addResult_forNoAction_withParticipation() {
        HealthTrackerStatus htStatus = new HealthTrackerStatus();

        CheckInResult result = new CheckInResult();
        result.setType(CheckInResult.ResultType.noAction);
        htStatus.addResult(result);

        CheckInResult result2 = new CheckInResult();
        result2.setTitle("P2");
        result2.setType(CheckInResult.ResultType.participation);
        htStatus.addResult(result2);

        assertTrue("Keeps NoAction if participation is added", htStatus.getRuleResults().contains(result));
        assertTrue("Adds participation if there is 1 noAction added.", htStatus.getRuleResults().contains(result2));
    }

    @Test
    public void addResult_DeleteNoAction_ifNonParticipantRuleAdded() {
        HealthTrackerStatus htStatus = new HealthTrackerStatus();

        CheckInResult result = new CheckInResult();
        result.setType(CheckInResult.ResultType.noAction);
        htStatus.addResult(result);

        CheckInResult result2 = new CheckInResult();
        result2.setTitle("symptom");
        result2.setType(CheckInResult.ResultType.symptom);
        htStatus.addResult(result2);

        assertFalse("Deletes NoAction if another rule is added", htStatus.getRuleResults().contains(result));
        assertTrue("Adds non-participation rule if there is 1 noAction added.", htStatus.getRuleResults().contains(result2));
    }

    @Test
    public void addResult_SkipAddingNoAction_ifNonParticipantRuleAlreadyPresent() {
        HealthTrackerStatus htStatus = new HealthTrackerStatus();

        CheckInResult result = new CheckInResult();
        result.setType(CheckInResult.ResultType.symptom);
        htStatus.addResult(result);

        CheckInResult result2 = new CheckInResult();
        result2.setType(CheckInResult.ResultType.noAction);
        htStatus.addResult(result2);

        assertTrue("Keeps participation rule", htStatus.getRuleResults().contains(result));
        assertFalse("Deos NOT add no action rule", htStatus.getRuleResults().contains(result2));
    }

    @Test
    public void addResult_ParticipationSymptomAdherenceStartDateUpdated_AllCanBeCombined() {
        HealthTrackerStatus htStatus = new HealthTrackerStatus();

        CheckInResult result = new CheckInResult();
        result.setType(CheckInResult.ResultType.symptom);
        htStatus.addResult(result);

        CheckInResult result2 = new CheckInResult();
        result2.setType(CheckInResult.ResultType.participation);
        htStatus.addResult(result2);

        CheckInResult result3 = new CheckInResult();
        result3.setType(CheckInResult.ResultType.startDateUpdated);
        htStatus.addResult(result3);

        CheckInResult result4 = new CheckInResult();
        result4.setType(CheckInResult.ResultType.action);
        htStatus.addResult(result4);

        CheckInResult result5 = new CheckInResult();
        result5.setType(CheckInResult.ResultType.adherence);
        htStatus.addResult(result5);

        assertEquals("includes all the result types", 5, htStatus.getRuleResults().size());
        assertTrue("Includes symptom", htStatus.getRuleResults().contains(result));
        assertTrue("Includes participation", htStatus.getRuleResults().contains(result2));
        assertTrue("Includes startDateUpdated", htStatus.getRuleResults().contains(result3));
        assertTrue("Includes action", htStatus.getRuleResults().contains(result4));
        assertTrue("Includes adherence", htStatus.getRuleResults().contains(result5));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void verifyRuleResultsCannotBeModified_viaGetRuleResults() {
        HealthTrackerStatus htStatus = new HealthTrackerStatus();

        Set<CheckInResult> ruleResults = htStatus.getRuleResults();

        ruleResults.add(new CheckInResult());
    }

}