package com.navigatingcancer.healthtracker.api.data.client;

import com.navigatingcancer.feign.utils.FeignUtils;
import com.navigatingcancer.healthtracker.api.data.model.documents.*;
import com.navigatingcancer.security.domain.Auth0Config;
import com.navigatingcancer.security.utils.SecurityUtils;
import feign.Param;
import feign.RequestLine;
import feign.RequestTemplate;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DocumentServiceClient {
  private static final int MAX_POLLING_RETRIES = 5;

  public interface FeignClient {
    @RequestLine("POST /clinic/{clinicId}/patient/{patientId}/document")
    DocumentRequestReceipt.Response requestDocumentCreation(
        DocumentRequest request,
        @Param("clinicId") Long clinicId,
        @Param("patientId") Long patientId);

    @RequestLine("GET /clinic/{clinicId}/patient/{patientId}/document/{documentId}/url")
    DocumentTemporaryUrl.Response getDocumentTempUrl(
        @Param("clinicId") Long clinicId,
        @Param("patientId") Long patientId,
        @Param("documentId") String documentId);

    @RequestLine("GET /clinic/{clinicId}/patient/{patientId}/document/{documentId}/status")
    DocumentStatus.Response getDocumentStatus(
        @Param("clinicId") Long clinicId,
        @Param("patientId") Long patientId,
        @Param("documentId") String documentId);
  }

  private final String baseUrl;
  private final Auth0Config auth0Config;

  @Autowired
  public DocumentServiceClient(
      Auth0Config auth0Config, @Value("${feign.ht_documents.url}") String baseUrl) {
    this.baseUrl = baseUrl;
    this.auth0Config = auth0Config;
  }

  /**
   * Requests creation of a PRO document for a given PRO review
   *
   * @param clinicId
   * @param patientId
   * @param proReviewId
   * @return the API response
   */
  public DocumentRequestReceipt requestProDocument(
      Long clinicId, Long patientId, String proReviewId) {

    DocumentRequest req = DocumentRequest.proDocumentRequest(clinicId, patientId, proReviewId);
    return this.getApi(baseUrl).requestDocumentCreation(req, clinicId, patientId).getData();
  }

  /**
   * Fetches the status for a given document.
   *
   * @param locator
   * @return the status
   */
  public DocumentStatus getDocumentStatus(DocumentLocator locator) {
    return this.getApi(baseUrl)
        .getDocumentStatus(locator.getClinicId(), locator.getPatientId(), locator.getDocumentId())
        .getData();
  }

  /**
   * Given a document locator, poll for the status of the document refereced by {@param locator}
   * until its status matches one of the values in {@param desiredStatuses}, or the
   * MAX_POLLING_RETRIES polling attempts have been made.
   *
   * @param locator
   * @param desiredStatuses set of statuses to wait for
   * @return a CompletableFuture with the requested status as a result, or an exceptionally
   *     completed future
   */
  @Async
  public CompletableFuture<DocumentStatus> pollForDocumentStatus(
      DocumentLocator locator, Set<String> desiredStatuses) {

    for (int retries = 1; retries <= MAX_POLLING_RETRIES; retries++) {
      DocumentStatus status = getDocumentStatus(locator);
      if (desiredStatuses.contains(status.getStatus())) {
        return CompletableFuture.completedFuture(status);
      }

      try {
        // exponential backoff, with min delay of 1 second, max delay of MAX_POLLING_RETRIES^2
        // seconds
        Thread.sleep((retries ^ 2) * 1000);
      } catch (InterruptedException e) {
        return CompletableFuture.failedFuture(e);
      }
    }

    return CompletableFuture.failedFuture(
        new Exception("document did not return expected status after max retries"));
  }

  /**
   * Requests a temporary URL for the specified document
   *
   * @param locator
   * @return the API response
   */
  public DocumentTemporaryUrl getDocumentTempUrl(DocumentLocator locator) {
    return this.getApi(baseUrl)
        .getDocumentTempUrl(locator.getClinicId(), locator.getPatientId(), locator.getDocumentId())
        .getData();
  }

  private FeignClient getApi(String url) {
    return FeignUtils.feign().requestInterceptor(this::setToken).target(FeignClient.class, url);
  }

  private void setToken(RequestTemplate i) {
    try {
      i.header(
          "Authorization",
          "Bearer "
              + SecurityUtils.getServiceToServiceAuth(
                  auth0Config.getDomain(),
                  auth0Config.getClientId(),
                  auth0Config.getClientSecret(),
                  auth0Config.getAudience()));
    } catch (Exception e) {
      log.error("unable to set access token for document service call", e);
    }
  }
}
