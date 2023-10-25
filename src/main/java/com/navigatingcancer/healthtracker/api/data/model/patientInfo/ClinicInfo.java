package com.navigatingcancer.healthtracker.api.data.model.patientInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClinicInfo {

  String name;
  String streetAddress1;
  String streetAddress2;
  String city;
  String state;
  String postalCode;
}
