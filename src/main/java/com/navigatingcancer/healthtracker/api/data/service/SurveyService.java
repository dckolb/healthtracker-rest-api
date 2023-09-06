package com.navigatingcancer.healthtracker.api.data.service;

import java.io.IOException;
import java.util.Map;

public interface SurveyService {
	
	Map<?, ?> getDefinition(String fileName) throws IOException;

}
