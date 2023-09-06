package com.navigatingcancer.healthtracker.api.data.service.impl;

import java.util.List;

import com.navigatingcancer.healthtracker.api.data.model.CallAttempt;
import com.navigatingcancer.healthtracker.api.data.repo.CallAttemptRepository;
import com.navigatingcancer.healthtracker.api.data.service.CallAttemptService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CallAttemptServiceImpl implements CallAttemptService {

	@Autowired
    private CallAttemptRepository callAttemptRepository;

    @Override
	public CallAttempt saveCallAttempt(CallAttempt callAttempt) {
		log.debug("CallAttemptService::saveCallAttempt");

		CallAttempt persisted = this.callAttemptRepository.insert(callAttempt);

		return persisted;
	}

    @Override
	public List<CallAttempt> getCallAttempts(List<String> checkInIds) {
		log.debug("CallAttemptService::getCallAttempts");

		List<CallAttempt> persisted = this.callAttemptRepository.getCallAttempts(checkInIds);

		return persisted;
	}
}
