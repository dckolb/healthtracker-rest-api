package com.navigatingcancer.healthtracker.api.data.validator;

import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyId;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.logging.log4j.util.Strings;

public class SurveyIdValidator implements ConstraintValidator<ValidSurveyId, String> {
  @Override
  public void initialize(ValidSurveyId constraintAnnotation) {}

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return Strings.isNotBlank(value) && SurveyId.isValid(value);
  }
}
