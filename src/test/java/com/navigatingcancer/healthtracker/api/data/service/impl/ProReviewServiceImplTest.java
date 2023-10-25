package com.navigatingcancer.healthtracker.api.data.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.navigatingcancer.healthtracker.api.data.client.DocumentServiceClient;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatus;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerStatusCategory;
import com.navigatingcancer.healthtracker.api.data.model.ProReview;
import com.navigatingcancer.healthtracker.api.data.model.ProReviewNote;
import com.navigatingcancer.healthtracker.api.data.model.documents.DocumentLocator;
import com.navigatingcancer.healthtracker.api.data.model.documents.DocumentRequestReceipt;
import com.navigatingcancer.healthtracker.api.data.model.documents.DocumentStatus;
import com.navigatingcancer.healthtracker.api.data.model.documents.DocumentTemporaryUrl;
import com.navigatingcancer.healthtracker.api.data.model.patientInfo.PatientInfo;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyPayload;
import com.navigatingcancer.healthtracker.api.data.repo.proReview.ProReviewRepository;
import com.navigatingcancer.healthtracker.api.data.repo.proReviewNote.ProReviewNoteRepository;
import com.navigatingcancer.healthtracker.api.events.HealthTrackerEventsPublisher;
import com.navigatingcancer.healthtracker.api.metrics.HealthTrackerCounterMetric;
import com.navigatingcancer.healthtracker.api.metrics.MetersService;
import com.navigatingcancer.healthtracker.api.processor.HealthTrackerStatusService;
import com.navigatingcancer.healthtracker.api.rest.exception.RecordNotFoundException;
import com.navigatingcancer.healthtracker.api.rest.representation.ProReviewResponse;
import com.navigatingcancer.healthtracker.api.rest.representation.ProReviewUpdateRequest;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ProReviewServiceImplTest {
  @MockBean ProReviewNoteRepository proReviewNoteRepository;
  @MockBean ProReviewRepository proReviewRepository;
  @MockBean HealthTrackerStatusService healthTrackerStatusService;
  @MockBean HealthTrackerEventsPublisher eventsPublisher;
  @MockBean DocumentServiceClient documentServiceClient;
  @MockBean RabbitTemplate rabbitTemplate;
  @MockBean MetersService metersService;
  @Captor private ArgumentCaptor<String> stringCaptor;
  @Captor private ArgumentCaptor<HealthTrackerStatusCategory> htStatusCategoryCaptor;
  @Captor private ArgumentCaptor<List<String>> stringListCaptor;
  @Captor private ArgumentCaptor<ProReviewNote> proReviewNoteCaptor;

  @Autowired private ProReviewServiceImpl proReviewService;

  @Before
  public void setupDocumentServiceMocks() {
    DocumentLocator fakeDocumentLocator = new DocumentLocator();
    fakeDocumentLocator.setPatientId(1L);
    fakeDocumentLocator.setClinicId(1L);
    DocumentRequestReceipt fakeReceipt = new DocumentRequestReceipt();
    fakeReceipt.setLocator(fakeDocumentLocator);
    fakeReceipt.setPath("bla");
    when(documentServiceClient.requestProDocument(any(), any(), any())).thenReturn(fakeReceipt);

    DocumentTemporaryUrl fakeTempUrl = new DocumentTemporaryUrl();
    when(documentServiceClient.getDocumentTempUrl(any())).thenReturn(fakeTempUrl);

    DocumentStatus fakeDocumentStatus = new DocumentStatus();
    fakeDocumentStatus.setStatus("created");
    when(documentServiceClient.pollForDocumentStatus(any(), any()))
        .thenReturn(CompletableFuture.completedFuture(fakeDocumentStatus));

    HealthTrackerStatus returnStatus = new HealthTrackerStatus();
    returnStatus.setClinicId(1L);
    PatientInfo ptInfo = new PatientInfo();
    ptInfo.setId(1L);
    returnStatus.setPatientInfo(ptInfo);
    returnStatus.setCategory(HealthTrackerStatusCategory.ACTION_NEEDED);
    given(healthTrackerStatusService.getById(any())).willReturn(returnStatus);
  }

  @Test
  public void getProReview_returnsFoundProReview() {
    ProReview proReview =
        new ProReview(
            "1",
            1L,
            "enrollmentId",
            Collections.emptyList(),
            new SurveyPayload(),
            Collections.emptyList(),
            Collections.emptyList(),
            null,
            null,
            null);

    given(proReviewRepository.findById("1")).willReturn(Optional.of(proReview));

    ProReviewNote proReviewNote1 = new ProReviewNote("1", "testContent", "dr test", new Date(1L));
    ProReviewNote proReviewNote2 = new ProReviewNote("1", "testContent2", "dr test", new Date(2L));
    List<ProReviewNote> notes = List.of(proReviewNote1, proReviewNote2);
    given(proReviewNoteRepository.getNotesByProReviewId("1")).willReturn(notes);

    ProReviewResponse proReviewResponse = proReviewService.getProReview("1");
    assertEquals(2, proReviewResponse.getNotes().size());
    assertEquals("1", proReviewResponse.getId());
  }

  @Test
  public void getProReview_throwsErrorIfNoneFound() {
    given(proReviewRepository.findById("1")).willReturn(Optional.empty());
    assertThrows(RecordNotFoundException.class, () -> this.proReviewService.getProReview("1"));
  }

  @Test
  public void processProReview_usesCorrectArgs() {
    given(proReviewNoteRepository.save(any())).willReturn(null);

    ProReviewUpdateRequest request =
        new ProReviewUpdateRequest(
            "1",
            List.of("1", "2"),
            "LoremIpsum",
            HealthTrackerStatusCategory.NO_ACTION_NEEDED,
            false,
            null);

    proReviewService.processProReview("3", request, "Dr Test");

    verify(healthTrackerStatusService)
        .setCategory(
            stringCaptor.capture(), htStatusCategoryCaptor.capture(), stringListCaptor.capture());
    verify(proReviewNoteRepository).save(proReviewNoteCaptor.capture());

    assertEquals("3", proReviewNoteCaptor.getValue().getProReviewId());
    assertEquals("LoremIpsum", proReviewNoteCaptor.getValue().getContent());
    assertEquals("Dr Test", proReviewNoteCaptor.getValue().getCreatedBy());
    assertEquals("1", stringCaptor.getValue());
    assertEquals(HealthTrackerStatusCategory.NO_ACTION_NEEDED, htStatusCategoryCaptor.getValue());
    assertEquals(request.getCheckInIds(), stringListCaptor.getValue());
  }

  @Test
  public void processProReview_doesNotCreateNullOrBlankNote() {
    ProReviewUpdateRequest nullNoteRequest =
        new ProReviewUpdateRequest(
            "1", List.of("1", "2"), null, HealthTrackerStatusCategory.ACTION_NEEDED, false, null);

    ProReviewUpdateRequest blankNoteRequest =
        new ProReviewUpdateRequest(
            "1", List.of("1", "2"), "  ", HealthTrackerStatusCategory.ACTION_NEEDED, false, null);

    proReviewService.processProReview("3", nullNoteRequest, "Dr Test");
    proReviewService.processProReview("3", blankNoteRequest, "Dr Test");
    verify(proReviewNoteRepository, times(0)).save(any());
  }

  @Test
  public void processProReview_doesNotSetCategoryIfNotNeeded() {
    ProReviewUpdateRequest request =
        new ProReviewUpdateRequest(
            "1",
            List.of("1", "2"),
            "LoremIpsum",
            HealthTrackerStatusCategory.ACTION_NEEDED,
            false,
            null);

    proReviewService.processProReview("3", request, "Dr Test");
    verify(healthTrackerStatusService, times(0)).setCategory(any(), any(), any());
  }

  @Test
  public void processProReview_errorsWithoutRequiredArgs() {
    ProReviewUpdateRequest request =
        new ProReviewUpdateRequest(
            "1",
            List.of("1", "2"),
            "LoremIpsum",
            HealthTrackerStatusCategory.ACTION_NEEDED,
            false,
            null);

    assertThrows(
        IllegalArgumentException.class,
        () -> proReviewService.processProReview(null, request, "Dr Test"));
    assertThrows(
        IllegalArgumentException.class,
        () -> proReviewService.processProReview("3", null, "Dr Test"));
    assertThrows(
        IllegalArgumentException.class,
        () -> proReviewService.processProReview("3", request, null));
  }

  @Test
  public void processProReview_publishesProSentEventWhenItShould() {
    boolean sendToEhr = true;

    ProReviewUpdateRequest request =
        new ProReviewUpdateRequest(
            "1",
            List.of("1", "2"),
            "LoremIpsum",
            HealthTrackerStatusCategory.ACTION_NEEDED,
            sendToEhr,
            null);

    proReviewService.processProReview("3", request, "Dr Test");
    verify(eventsPublisher, times(1))
        .publishProSentToEhr(
            anyString(), anyString(), anyLong(), anyLong(), anyList(), anyString());
  }

  @Test
  public void processProReview_doesNOTPublishProSentEventWhenItShouldNot() {
    boolean sendToEhr = false;

    ProReviewUpdateRequest request =
        new ProReviewUpdateRequest(
            "1",
            List.of("1", "2"),
            "LoremIpsum",
            HealthTrackerStatusCategory.ACTION_NEEDED,
            sendToEhr,
            null);

    proReviewService.processProReview("3", request, "Dr Test");
    verify(eventsPublisher, times(0))
        .publishProSentToEhr(
            anyString(), anyString(), anyLong(), anyLong(), anyList(), anyString());
  }

  @Test
  public void processProReview_publishesProNoteEventWhenItShould() {
    boolean sendToEhr = false;

    ProReviewUpdateRequest request =
        new ProReviewUpdateRequest(
            "1",
            List.of("1", "2"),
            "LoremIpsum",
            HealthTrackerStatusCategory.ACTION_NEEDED,
            sendToEhr,
            null);

    proReviewService.processProReview("3", request, "Dr Test");
    verify(eventsPublisher, times(1))
        .publishProReviewNote(
            anyString(), anyString(), anyLong(), anyLong(), anyList(), anyString(), anyString());
  }

  @Test
  public void processProReview_doesNOTpublishProReviewNoteEventWhenItShouldNot() {
    boolean sendToEhr = true;

    ProReviewUpdateRequest request =
        new ProReviewUpdateRequest(
            "1", List.of("1", "2"), "", HealthTrackerStatusCategory.ACTION_NEEDED, sendToEhr, null);

    proReviewService.processProReview("3", request, "Dr Test");
    verify(eventsPublisher, times(0))
        .publishProReviewNote(
            anyString(), anyString(), anyLong(), anyLong(), anyList(), anyString(), anyString());
  }

  @Test
  public void processProReview_incrementsMetrics() {
    ProReviewUpdateRequest request =
        new ProReviewUpdateRequest(
            "1",
            List.of("1", "2"),
            "a note!",
            HealthTrackerStatusCategory.ACTION_NEEDED,
            true,
            null);

    proReviewService.processProReview("3", request, "Dr Test");

    verify(metersService).incrementCounter(1L, HealthTrackerCounterMetric.PRO_SENT_TO_EHR);
    verify(metersService).incrementCounter(1L, HealthTrackerCounterMetric.PRO_NOTE_ADDED);
  }

  @Test
  public void processProReview_doesNotSendEventWhenDocumentStatusIsError() {
    DocumentStatus fakeStatus = new DocumentStatus();
    fakeStatus.setStatus("error");

    when(documentServiceClient.pollForDocumentStatus(any(), any()))
        .thenReturn(CompletableFuture.completedFuture(fakeStatus));

    ProReviewUpdateRequest request =
        new ProReviewUpdateRequest(
            "1",
            List.of("1", "2"),
            "a note!",
            HealthTrackerStatusCategory.ACTION_NEEDED,
            true,
            null);

    proReviewService.processProReview("3", request, "Dr Test");

    verify(eventsPublisher, times(0))
        .publishProSentToEhr(
            anyString(), anyString(), anyLong(), anyLong(), anyList(), anyString());
    verify(metersService).incrementCounter(1L, HealthTrackerCounterMetric.PRO_DOCUMENT_ERROR);
  }

  @Test
  public void processProReview_doesNotSendEventWhenDocumentStatusCheckResultsInFailure() {
    DocumentStatus fakeStatus = new DocumentStatus();
    fakeStatus.setStatus("error");

    when(documentServiceClient.pollForDocumentStatus(any(), any()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("it failed!")));

    ProReviewUpdateRequest request =
        new ProReviewUpdateRequest(
            "1",
            List.of("1", "2"),
            "a note!",
            HealthTrackerStatusCategory.ACTION_NEEDED,
            true,
            null);

    proReviewService.processProReview("3", request, "Dr Test");

    verify(eventsPublisher, times(0))
        .publishProSentToEhr(
            anyString(), anyString(), anyLong(), anyLong(), anyList(), anyString());
    verify(metersService).incrementCounter(1L, HealthTrackerCounterMetric.PRO_DOCUMENT_ERROR);
  }
}
