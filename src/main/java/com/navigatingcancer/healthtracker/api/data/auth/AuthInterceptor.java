package com.navigatingcancer.healthtracker.api.data.auth;

import static java.lang.Long.parseLong;

import java.util.Enumeration;
import java.util.Objects;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

  public static final String HEALTH_TRACKER_NAME = "Health Tracker";

  private static final String PATIENT_ID_HEADER = "NC-PATIENT-ID";
  private static final String LOCATION_ID_HEADER = "NC-LOCATION-ID";
  private static final String CLINIC_ID_HEADER = "NC-CLINIC-ID";
  private static final String CLINICIAN_NAME_HEADER = "Clinician-Name";
  private static final String CLINICIAN_ID_HEADER = "Clinician-Identity-Id";
  private static final String NC_CLINICIAN_NAME = "NC-CLINICIAN-NAME";
  private static final String NC_CLINICIAN_ID = "NC-CLINICIAN-ID";

  private static final String PATIENT_ID_MDC_KEY = "patientId";
  private static final String CLINIC_ID_MDC_KEY = "clinicId";
  private static final String LOCATION_ID_MDC_KEY = "locationId";
  private static final String CLINICIAN_ID_MDC_KEY = "clinicianId";

  @Autowired private Identity identity;

  @Autowired private IdentityContextHolder identityContextHolder;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {

    mapRequestToIdentity(request);
    addIdentityToMDC();
    logRequest(request);

    // set context in threadlocal
    IdentityContext identityContext =
        new IdentityContext(
            identity.getClinicianName(),
            identity.getClinicianId(),
            identity.getPatientId(),
            identity.getClinicId(),
            identity.getLocationId());
    log.debug("Setting identity context {}", identityContext);
    identityContextHolder.set(identityContext);

    return HandlerInterceptor.super.preHandle(request, response, handler);
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    removeIdentityContextFromMDC();
  }

  private void mapRequestToIdentity(HttpServletRequest request) {
    identity.setClinicianId(
        firstNotBlank(request.getHeader(NC_CLINICIAN_ID), request.getHeader(CLINICIAN_ID_HEADER)));
    identity.setClinicianName(
        firstNotBlank(
            request.getHeader(NC_CLINICIAN_NAME),
            request.getHeader(CLINICIAN_NAME_HEADER),
            HEALTH_TRACKER_NAME));
    identity.setLocationId(tryParseLong(request.getHeader(LOCATION_ID_HEADER)));
    identity.setClinicId(tryParseLong(request.getHeader(CLINIC_ID_HEADER)));
    identity.setPatientId(tryParseLong(request.getHeader(PATIENT_ID_HEADER)));

    if (Stream.of(identity.getPatientId(), identity.getLocationId(), identity.getClinicId())
        .allMatch(Objects::nonNull)) {
      identity.setSet(true);

      log.info("Extracted identity {}", identity);
    }
  }

  private static String firstNotBlank(String... vals) {
    return Stream.of(vals).filter(StringUtils::isNotBlank).findFirst().orElse(null);
  }

  private static Long tryParseLong(String val) {
    Long parsed = null;
    if (!StringUtils.isBlank(val)) {
      parsed = parseLong(val);
    }
    return parsed;
  }

  private static void logRequest(HttpServletRequest request) {
    log.debug(request.getRequestURI() + "?" + request.getQueryString());

    Enumeration<String> it = request.getHeaderNames();
    while (it.hasMoreElements()) {
      String name = it.nextElement();
      log.debug("Header {}={}", name, request.getHeader(name));
    }
  }

  /**
   * Store identity fields in the MDC (Mapped Diagnostic Context) for inclusion in logs. See
   * https://logback.qos.ch/manual/mdc.html for details on MDC.
   */
  private void addIdentityToMDC() {
    if (identity.getPatientId() != null) {
      MDC.put(PATIENT_ID_MDC_KEY, Long.toString(identity.getPatientId()));
    }
    if (identity.getClinicId() != null) {
      MDC.put(CLINIC_ID_MDC_KEY, Long.toString(identity.getClinicId()));
    }
    if (identity.getLocationId() != null) {
      MDC.put(LOCATION_ID_MDC_KEY, Long.toString(identity.getLocationId()));
    }
    if (!StringUtils.isBlank(identity.getClinicianId())) {
      MDC.put(CLINICIAN_ID_MDC_KEY, identity.getClinicianId());
    }
    log.debug("Added identity to MDC");
  }

  private static void removeIdentityContextFromMDC() {
    MDC.remove(PATIENT_ID_MDC_KEY);
    MDC.remove(LOCATION_ID_MDC_KEY);
    MDC.remove(CLINIC_ID_MDC_KEY);
    MDC.remove(CLINICIAN_ID_MDC_KEY);
  }
}
