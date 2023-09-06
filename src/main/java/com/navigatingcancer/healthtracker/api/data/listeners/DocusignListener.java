package com.navigatingcancer.healthtracker.api.data.listeners;

import com.navigatingcancer.healthtracker.api.data.repo.EnrollmentRepository;
import com.navigatingcancer.sqs.SqsListener;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(value = "docusign.enabled", havingValue = "true")
@Service
@SqsListener(queueName = DocusignListener.DOCUSIGN_OUTBOUND_QUEUE)
@Slf4j
public class DocusignListener implements Consumer<SigningRequestEvent> {

    @Value(DOCUSIGN_OUTBOUND_QUEUE)
    String queueName;

    static final String DOCUSIGN_OUTBOUND_QUEUE = "${docusign.outbound}";

    private EnrollmentRepository enrollmentRepository;

    @Autowired
    public DocusignListener(EnrollmentRepository enrollmentRepository){
        log.debug("DocusignListener");
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public void accept(SigningRequestEvent signingRequestEvent) {
        log.debug("DocusignListener::accept");
        log.debug("SigningRequestEvent received {}", signingRequestEvent);
        // update enrollment with this request id
        // with status and date
        this.enrollmentRepository.updateConsentStatus(
                signingRequestEvent.getSigningRequestId(),
                signingRequestEvent.getStatus(),
                signingRequestEvent.getTimestamp());
    }
}
