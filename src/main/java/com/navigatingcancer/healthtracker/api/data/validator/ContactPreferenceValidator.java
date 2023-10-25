package com.navigatingcancer.healthtracker.api.data.validator;

import com.navigatingcancer.healthtracker.api.data.model.ContactPreferences;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class ContactPreferenceValidator
    implements ConstraintValidator<ValidContactPreferences, ContactPreferences> {

  @Override
  public void initialize(ValidContactPreferences constraintAnnotation) {}

  @Override
  public boolean isValid(ContactPreferences value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    if (StringUtils.isBlank(value.getEmailAddress())) {
      return StringUtils.isNotBlank(value.getPhoneNumber());
    }

    return StringUtils.isNotBlank(value.getEmailAddress());
  }
}
