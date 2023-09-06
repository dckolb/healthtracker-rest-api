package com.navigatingcancer.healthtracker.api.data.auth;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class Identity {

	Long locationId;
	Long patientId;
	Long clinicId;
	boolean isSet;
	String clinicianId;
	String clinicianName;

}
