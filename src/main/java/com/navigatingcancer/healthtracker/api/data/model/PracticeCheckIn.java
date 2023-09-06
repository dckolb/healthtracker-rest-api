package com.navigatingcancer.healthtracker.api.data.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;



@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Document(collection = "practice_checkins")
public class PracticeCheckIn extends AbstractDocument {
    public enum Status {
        COMPLETED,
        DECLINED
    }

    private Long clinicId;
    private Long patientId;
    private String completedBy;
    private Status status;
}
