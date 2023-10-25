package com.navigatingcancer.healthtracker.api.data.validator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = SurveyIdValidator.class)
public @interface ValidSurveyId {
  String message() default "Only known survey IDs are permitted";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
