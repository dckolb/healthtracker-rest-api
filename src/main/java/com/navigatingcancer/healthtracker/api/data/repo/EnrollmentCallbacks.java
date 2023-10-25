package com.navigatingcancer.healthtracker.api.data.repo;

import com.navigatingcancer.healthtracker.api.data.model.Enrollment;
import java.util.ArrayList;
import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertCallback;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentCallbacks implements AfterConvertCallback<Enrollment> {
  @Override
  public Enrollment onAfterConvert(Enrollment enrollment, Document document, String collection) {
    if (enrollment.getSchedules() == null) {
      enrollment.setSchedules(new ArrayList<>());
    } else {
      enrollment
          .getSchedules()
          .forEach(
              schedule -> {
                schedule.setEnrollmentId(enrollment.getId());
              });
    }
    return enrollment;
  }
}
