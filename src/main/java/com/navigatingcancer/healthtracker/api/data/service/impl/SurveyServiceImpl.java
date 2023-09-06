package com.navigatingcancer.healthtracker.api.data.service.impl;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.navigatingcancer.healthtracker.api.data.service.SurveyService;
import com.navigatingcancer.json.utils.JsonUtils;

@Service
public class SurveyServiceImpl implements SurveyService {
	
	@Value("${survey.definition.bucket}")
	String surveyBucket;
	
	AmazonS3 client;

	@Override
	public Map<?, ?> getDefinition(String fileName) throws IOException {
		return JsonUtils.fromJson(client().getObjectAsString(surveyBucket, fileName), Map.class);
	}
	
	private AmazonS3 client() {
		if (client == null) {
			client = AmazonS3ClientBuilder.standard().build();
		}
		return client;
	}

}
