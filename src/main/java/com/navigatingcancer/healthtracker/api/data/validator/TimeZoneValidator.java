package com.navigatingcancer.healthtracker.api.data.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TimeZoneValidatorImpl.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface TimeZoneValidator {
    String message() default "is not a valid timezone";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
