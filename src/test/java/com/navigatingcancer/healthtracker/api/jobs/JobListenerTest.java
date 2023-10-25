package com.navigatingcancer.healthtracker.api.jobs;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class JobListenerTest {
  private JobListener listener;
  private JobPublisher publisher;
  private final int maxRetries = 3;

  @Before
  public void setUp() {
    publisher = mock(JobPublisher.class);
    listener =
        new JobListener(
            maxRetries,
            List.of(
                new NoOpJobTemplate(),
                new AlwaysFailingJobTemplate(),
                new UnretryableAlwaysFailingJobTemplate()),
            publisher);
  }

  @Test
  public void accept_happilyExecutesNoOpJob() {
    listener.accept(new NoOpJobTemplate.Payload());
    verifyNoInteractions(publisher);
  }

  @Test
  public void accept_reQueuesAlwaysFailingRetryableJob() {
    var payload = new AlwaysFailingJobTemplate.Payload();
    listener.accept(payload);
    verify(publisher, times(1)).publish(payload);
    assertEquals(payload.getRetryCount(), 1);
    assertEquals(payload.getMessage(), "something went wrong");
  }

  @Test(expected = RuntimeException.class)
  public void accept_doesntReQueueOnUnRetryableException() {
    var payload = new UnretryableAlwaysFailingJobTemplate.Payload();
    listener.accept(payload);
  }

  @Test(expected = RuntimeException.class)
  public void accept_throwsWhenAlwaysFailingJobExceedsRetries() {
    var payload = new AlwaysFailingJobTemplate.Payload();
    payload.setRetryCount(maxRetries);
    listener.accept(payload);
  }

  static class NoOpJobTemplate implements JobTemplate<NoOpJobTemplate.Payload> {
    static class Payload extends JobPayload {
      @Override
      String getJobType() {
        return "noop";
      }
    }

    @Override
    public Class<NoOpJobTemplate.Payload> getPayloadType() {
      return Payload.class;
    }

    @Override
    public void execute(Payload payload) {
      // do nothing
    }
  }

  static class AlwaysFailingJobTemplate implements JobTemplate<AlwaysFailingJobTemplate.Payload> {
    static class Payload extends JobPayload {
      @Override
      String getJobType() {
        return "alwaysFailsRetryable";
      }
    }

    @Override
    public Class<AlwaysFailingJobTemplate.Payload> getPayloadType() {
      return Payload.class;
    }

    @Override
    public void execute(Payload payload) throws RetryableJobException {
      throw new RetryableJobException("something went wrong");
    }
  }

  static class UnretryableAlwaysFailingJobTemplate
      implements JobTemplate<UnretryableAlwaysFailingJobTemplate.Payload> {
    static class Payload extends JobPayload {
      @Override
      String getJobType() {
        return "alwaysFailsUnretryable";
      }
    }

    @Override
    public Class<UnretryableAlwaysFailingJobTemplate.Payload> getPayloadType() {
      return Payload.class;
    }

    @Override
    public void execute(Payload payload) {
      throw new RuntimeException("something went wrong");
    }
  }
}
