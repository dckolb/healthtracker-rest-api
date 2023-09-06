package com.navigatingcancer.healthtracker.api.rest.QueryParameters;

import com.navigatingcancer.healthtracker.api.data.model.EnrollmentStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ToString
public class EnrollmentQuery {
    private static Logger logger = LoggerFactory.getLogger(EnrollmentQuery.class);

    private List<String> id = new ArrayList<>();
    private List<Long> locationId = new ArrayList<>();
    private List<Long> patientId = new ArrayList<>();
    private List<Long> clinicId = new ArrayList<>();
    private List<EnrollmentStatus> status = new ArrayList<>();
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean all = false;

    public boolean isValid(){
        logger.debug(this.toString());
        return (id != null && id.size() > 0) ||
                (locationId != null && locationId.size() > 0) ||
                (patientId != null && patientId.size() > 0)||
                (clinicId != null && clinicId.size() > 0) ||
                (status != null && status.size() > 0) ||
                (startDate != null && endDate != null &&
                        (startDate.isBefore(endDate) || startDate.isEqual(endDate)));
    }
}
