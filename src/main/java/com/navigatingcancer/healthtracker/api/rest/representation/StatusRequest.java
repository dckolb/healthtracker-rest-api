package com.navigatingcancer.healthtracker.api.rest.representation;

import lombok.Data;


import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;


@Data
public class StatusRequest {

    @NotNull
    Long clinicId;

    @NotEmpty
    List<String> ids;
}
