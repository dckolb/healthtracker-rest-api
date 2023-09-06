package com.navigatingcancer.healthtracker.api.data.auth;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class IdentityContext {

    @Getter
    private String clinicianName;
    @Getter
    private String clinicianId;
    @Getter
    private Long patientId;
    @Getter
    private Long clinicId;
    @Getter
    private Long locationId;

}
