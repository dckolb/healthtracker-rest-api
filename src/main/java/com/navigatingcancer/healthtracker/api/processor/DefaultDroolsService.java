package com.navigatingcancer.healthtracker.api.processor;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.processor.model.DroolsManager;
import com.navigatingcancer.healthtracker.api.processor.model.HealthTracker;

import org.drools.core.event.DefaultAgendaEventListener;
import org.kie.api.definition.rule.Rule;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DefaultDroolsService {

	@Autowired
	private DroolsManager droolsManager;
	
	class AgendaEventListener extends DefaultAgendaEventListener {
		HealthTracker ht;
		
		public AgendaEventListener(HealthTracker ht) {
			this.ht = ht;
		}

		@Override
		public void beforeMatchFired(BeforeMatchFiredEvent event) {
			Rule rule = event.getMatch().getRule();
			log.info("Rule to be tried : {}", rule);
		}

		@Override
		public void afterMatchFired(AfterMatchFiredEvent event) {
			Rule rule = event.getMatch().getRule();
			log.info("Rule fired : {}", rule);
		    // FIXME : should we not add NoAction rule name if that one is going to be overridden ?
			ht.getHealthTrackerStatus().getRuleNames().add(event.getMatch().getRule().getName());
		}

	}

	public HealthTrackerStatus process(HealthTracker ht, boolean withSurvey) {
		Enrollment e = ht.getEnrollment();
		KieContainer kieContainer;
		if( withSurvey ) {
			kieContainer = droolsManager.getRulesForClinicAndTreatmentType(e);
		} else {
			kieContainer = droolsManager.getRulesForStatusCheck(e);
		}
		KieSession kieSession = kieContainer.newKieSession();

		AgendaEventListener eventListener = new AgendaEventListener(ht);

		kieSession.addEventListener(eventListener);
		kieSession.insert(ht);
		kieSession.fireAllRules();
		ht.getHealthTrackerStatus().setMissedCheckIns(ht.getCheckInAggregator().countMissed());
		kieSession.dispose();

		return ht.getHealthTrackerStatus();
	}

	// Note. Keeping this call for historical reasons.
	public HealthTrackerStatus process(HealthTracker ht) {
		return process(ht, true);
	}
}
