package com.navigatingcancer.healthtracker.api.data.model;

import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
@Deprecated
public class CheckInData {

  Enrollment enrollment;
  List<CheckIn> missed;
  List<CheckIn> pending;
  CheckIn next;
  Integer completedCount;
  Integer totalCount;
  Integer adherencePercent;
  PatientInfo user;
  LocalDateTime nextCheckIn;
  LocalDateTime lastCheckIn;
  LocalDateTime nextOralCheckIn;
  LocalDateTime nextSymptomCheckIn;
  LocalDateTime lastOralCheckIn;
  LocalDateTime lastSymptomCheckIn;
  Boolean isProCtcaeFormat;

  public CheckInData() {
    this.isProCtcaeFormat = false; // default to false
  }

  public Boolean getIsFirst() {
    if (completedCount == null) return true;
    if (completedCount.equals(0)) return true;
    if (completedCount > 0
        && adherencePercent.equals(0)
        && pending.size() > 0
        && (pending.get(0).getCheckInType() == CheckInType.COMBO
            || pending.get(0).getCheckInType() == CheckInType.ORAL)) return true;

    // NEW ORAL START
    if (enrollment.getFirstCheckInResponseDate() != null
        && enrollment.getPatientReportedTxStartDate() == null
        && needsStartDate(pending)) return true;

    return false;
  }

  private boolean needsStartDate(List<CheckIn> pending) {
    for (CheckIn c : pending) {
      if (c.getCheckInType() == CheckInType.COMBO) return true;
      if (c.getCheckInType() == CheckInType.ORAL) return true;
    }
    return false;
  }
}
