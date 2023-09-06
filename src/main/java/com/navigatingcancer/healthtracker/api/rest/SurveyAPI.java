package com.navigatingcancer.healthtracker.api.rest;

import java.io.IOException;
import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(value = "survey")
public interface SurveyAPI {

	@ApiOperation(value = "", nickname = "surveyDefinition", notes = "", tags = { "survey", })
	@ApiResponses(value = { @ApiResponse(code = 200, message = ""), @ApiResponse(code = 405, message = "") })
	@RequestMapping(value = "/survey/{fileName}", method = RequestMethod.GET)
	Map<?, ?> getDefinition(@PathVariable("fileName") String fileName) throws IOException;

}
