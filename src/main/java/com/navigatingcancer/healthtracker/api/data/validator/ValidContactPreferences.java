package com.navigatingcancer.healthtracker.api.data.validator;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = ContactPreferenceValidator.class)
public @interface ValidContactPreferences {
  String message() default "Only valid contact preferences are permitted";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
