package com.navigatingcancer.healthtracker.api.data.model;

import com.navigatingcancer.healthtracker.api.data.validator.ValidContactPreferences;
import lombok.Data;

@Data
@ValidContactPreferences
public class ContactPreferences {
  private String emailAddress;
  private String phoneNumber;
}
