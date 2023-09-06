package com.navigatingcancer.healthtracker.api.data.validator;

import com.navigatingcancer.healthtracker.api.data.util.DateUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class TimeZoneValidatorImpl implements
        ConstraintValidator<TimeZoneValidator, String> {

    @Override
    public void initialize(TimeZoneValidator constraintAnnotation) {

    }

    /**
     * Implements the validation logic.
     * The state of {@code value} must not be altered.
     * <p>
     * This method can be accessed concurrently, thread-safety must be ensured
     * by the implementation.
     *
     * @param value   object to validate
     * @param context context in which the constraint is evaluated
     * @return {@code false} if {@code value} does not pass the constraint
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return DateUtils.validTimeZone(value);
    }
}
