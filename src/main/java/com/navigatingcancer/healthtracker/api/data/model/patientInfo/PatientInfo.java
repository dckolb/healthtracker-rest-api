package com.navigatingcancer.healthtracker.api.data.model.patientInfo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatientInfo {

  Long id;
  String mrn;
  String firstName;
  String lastName;
  String gender;
  String email;
  Boolean highRisk;

  @JsonFormat(pattern = "dd MMM yyyy")
  LocalDate birthdate;

  String homePhoneNumber;
  String cellPhoneNumber;
  String workPhoneNumber;
}
