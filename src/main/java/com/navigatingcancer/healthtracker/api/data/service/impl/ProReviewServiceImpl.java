package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.navigatingcancer.healthtracker.api.data.client.DocumentServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.EHRDelivery;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.model.PdfDeliveryStatus;
import com.navigatingcancer.healthtracker.api.data.model.ProReview;
import com.navigatingcancer.healthtracker.api.data.model.ProReviewNote;
import com.navigatingcancer.healthtracker.api.data.model.documents.DocumentLocator;
import com.navigatingcancer.healthtracker.api.data.model.documents.DocumentRequestReceipt;
import com.navigatingcancer.healthtracker.api.data.model.documents.DocumentTemporaryUrl;
import com.navigatingcancer.healthtracker.api.data.repo.proReview.ProReviewRepository;
import com.navigatingcancer.healthtracker.api.data.repo.proReviewNote.ProReviewNoteRepository;
import com.navigatingcancer.healthtracker.api.data.service.ProReviewService;
import com.navigatingcancer.healthtracker.api.events.HealthTrackerEventsPublisher;
import com.navigatingcancer.healthtracker.api.metrics.HealthTrackerCounterMetric;
import com.navigatingcancer.healthtracker.api.metrics.MetersService;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.healthtracker.api.rest.exception.RecordNotFoundException;
import com.navigatingcancer.healthtracker.api.rest.representation.ProReviewResponse;
import com.navigatingcancer.healthtracker.api.rest.representation.ProReviewUpdateRequest;
import com.navigatingcancer.json.utils.JsonUtils;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProReviewServiceImpl implements ProReviewService {
  private static final String PRO_DOCUMENT_ROUTING_KEY = "app/health_tracker/documents/pro";
  private static final String PRO_DOCUMENT_TITLE = "Health Tracker PRO";

  @Autowired private ProReviewRepository proReviewRepository;
  @Autowired private ProReviewNoteRepository proReviewNoteRepository;
  @Autowired private HealthTrackerStatusService healthTrackerStatusService;
  @Autowired private HealthTrackerEventsPublisher eventsPublisher;
  @Autowired private DocumentServiceClient documentServiceClient;
  @Autowired private RabbitTemplate rabbitTemplate;
  @Autowired private PatientRecordService patientRecordService;
  @Autowired private MetersService meterService;

  public ProReviewResponse getProReview(String id) {
    Optional<ProReview> proReview = proReviewRepository.findById(id);
    if (proReview.isEmpty()) {
      throw new RecordNotFoundException("Unknown ProReview ID");
    }

    List<ProReviewNote> proReviewNotes = proReviewNoteRepository.getNotesByProReviewId(id);

    return new ProReviewResponse(proReview.get(), proReviewNotes);
  }

  public HealthTrackerStatus processProReview(
      @NotNull String proReviewId,
      @NotNull ProReviewUpdateRequest request,
      @NotNull String createdBy)
      throws IllegalArgumentException, RecordNotFoundException {

    if (proReviewId == null || request == null || createdBy == null) {
      throw new IllegalArgumentException("processProReview called without required arguments");
    }

    log.info("Processing pro review {}", request);

    HealthTrackerStatus status = healthTrackerStatusService.getById(request.getEnrollmentId());
    if (status == null) {
      throw new RecordNotFoundException(
          "Unable to find Health Tracker Status with provided enrollmentId");
    }

    /*
      bugfix for HT-4498
      When a new enrollment is created and a patient submits their first survey,
      if the rules engine determines that no action is needed, the htStatus
      category is cleared from 'PENDING' to null and never set to 'NO_ACTION_NEEDED'.
      It is assumed that null category values are equivalent to 'NO_ACTION_NEEDED' in HealthTrackerEventsPublisher.
      prepareStatusChangeEvent.This seems like an anti-pattern and should be explored further.
    */
    if (status.getCategory() == null) {
      status.setCategory(HealthTrackerStatusCategory.NO_ACTION_NEEDED);
    }

    if (request.getPatientActivityId() != null) {
      proReviewRepository.appendPatientActivityId(proReviewId, request.getPatientActivityId());
    }

    if (request.getCategory() != null && status.getCategory() != request.getCategory()) {
      healthTrackerStatusService.setCategory(
          request.getEnrollmentId(), request.getCategory(), request.getCheckInIds());
    }

    if (request.getNoteContent() != null && !request.getNoteContent().isBlank()) {
      proReviewNoteRepository.save(
          new ProReviewNote(proReviewId, request.getNoteContent(), createdBy, new Date()));

      eventsPublisher.publishProReviewNote(
          request.getEnrollmentId(),
          createdBy,
          status.getClinicId(),
          status.getPatientInfo().getId(),
          request.getCheckInIds(),
          proReviewId,
          request.getNoteContent());

      meterService.incrementCounter(
          status.getClinicId(), HealthTrackerCounterMetric.PRO_NOTE_ADDED);
    }

    if (request.isSendToEhr()) {
      BackgroundEhrDeliverer ehrDelivery =
          new BackgroundEhrDeliverer(status, proReviewId, request, createdBy);
      ehrDelivery.execute();
    }

    return status;
  }

  public void markEhrDelivered(String proReviewId, String createdBy, String documentId) {
    proReviewRepository.updateEhrDeliveryById(
        proReviewId,
        new EHRDelivery(PdfDeliveryStatus.DELIVERED, documentId, createdBy, new Date(), null));
  }

  /**
   * Helper class to handle delivery document service polling and EHR delivery in background thread.
   */
  @Data
  @AllArgsConstructor
  private class BackgroundEhrDeliverer {
    final HealthTrackerStatus status;
    final String proReviewId;
    final ProReviewUpdateRequest request;
    final String createdBy;

    /**
     * Request document creation, mark the delivery as pending, then in a background thread poll for
     * the document status, delivering it to the clinic's EHR via RabbitMQ when successful.
     */
    void execute() {
      final DocumentRequestReceipt receipt;
      try {
        receipt =
            documentServiceClient.requestProDocument(
                status.getClinicId(), status.getPatientInfo().getId(), proReviewId);

        proReviewRepository.updateEhrDeliveryById(
            proReviewId,
            new EHRDelivery(
                PdfDeliveryStatus.PENDING_GENERATION,
                receipt.getLocator().getDocumentId(),
                createdBy,
                new Date(),
                null));

      } catch (Exception e) {
        handleEhrDeliveryError(e, null);
        throw e;
      }

      // poll for the document status, and upload the document asynchronously
      documentServiceClient
          .pollForDocumentStatus(receipt.getLocator(), Set.of("created", "error"))
          .whenComplete(
              (documentStatus, statusException) -> {
                if (statusException != null) {
                  handleEhrDeliveryError(statusException, null);
                  return;
                }

                if ("error".equalsIgnoreCase(documentStatus.getStatus())) {
                  handleEhrDeliveryError(
                      new RuntimeException(documentStatus.getErrorDetails()), receipt.getLocator());
                  return;
                }

                try {
                  deliverDocumentToEhr(receipt.getLocator());
                } catch (Exception e) {
                  handleEhrDeliveryError(e, receipt.getLocator());
                  return;
                }

                handleEhrDeliverySuccess(proReviewId, createdBy, receipt.getLocator(), request);
              });
    }

    private void deliverDocumentToEhr(DocumentLocator locator) {
      log.info("Preparing document for EHR upload, locator: {}", locator);

      DocumentTemporaryUrl tempUrl = documentServiceClient.getDocumentTempUrl(locator);

      DocumentUploadPayload payload =
          DocumentUploadPayload.builder()
              .clinicId(locator.getClinicId())
              .patientId(locator.getPatientId())
              .documentTitle(PRO_DOCUMENT_TITLE)
              .documentUrl(tempUrl.getUrl())
              .build();

      try {
        rabbitTemplate.convertAndSend(PRO_DOCUMENT_ROUTING_KEY, JsonUtils.toJson(payload));
      } catch (Exception e) {
        log.error("Unable to publish PRO document to rabbitmq ", e);
        throw e;
      }

      log.info(
          "Successfully published PRO document to RabbitMQ with routing key {}",
          PRO_DOCUMENT_ROUTING_KEY);
    }

    private void handleEhrDeliveryError(Throwable t, DocumentLocator locator) {
      log.error("EHR delivery failure", t);

      proReviewRepository.updateEhrDeliveryById(
          proReviewId,
          new EHRDelivery(
              PdfDeliveryStatus.ERROR,
              locator != null ? locator.getDocumentId() : null,
              createdBy,
              new Date(),
              t.getMessage()));

      meterService.incrementCounter(
          status.getClinicId(), HealthTrackerCounterMetric.PRO_DOCUMENT_ERROR);
    }

    private void handleEhrDeliverySuccess(
        String proReviewId,
        String createdBy,
        DocumentLocator locator,
        ProReviewUpdateRequest request) {

      log.info("EHR delivery successful for document {}", locator);

      markEhrDelivered(proReviewId, createdBy, locator.getDocumentId());

      patientRecordService.publishProSentToEhr(
          request.getEnrollmentId(),
          createdBy,
          locator.getClinicId(),
          locator.getPatientId(),
          proReviewId,
          PRO_DOCUMENT_TITLE);

      eventsPublisher.publishProSentToEhr(
          request.getEnrollmentId(),
          createdBy,
          locator.getClinicId(),
          locator.getPatientId(),
          request.getCheckInIds(),
          proReviewId);

      meterService.incrementCounter(
          locator.getClinicId(), HealthTrackerCounterMetric.PRO_SENT_TO_EHR);
    }
  }
}
