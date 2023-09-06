package com.navigatingcancer.healthtracker.api.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;
import io.micrometer.statsd.StatsdMeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Service;

@Data
@EqualsAndHashCode
class ClinicCounterKey {
  private final HealthTrackerCounterMetric metric;
  private final Long clinicId;
}

@Service
public class MetersService {
  private final Map<ClinicCounterKey, Counter> counterCache = new ConcurrentHashMap<>();
  private MeterRegistry meterRegistry;
  private String environment;
  private String serviceName;
  private String statsdAgentHost;

  /**
   * Increments a counter metric by 1 for the given clinic
   *
   * @param metric
   * @param clinicId
   */
  public void incrementCounter(Long clinicId, HealthTrackerCounterMetric metric) {
    getClinicCounter(clinicId, metric).increment();
  }

  /**
   * Increments a counter metric by amount for the given clinic
   *
   * @param clinicId
   * @param metric
   * @param amount
   */
  public void incrementCounter(Long clinicId, HealthTrackerCounterMetric metric, double amount) {
    getClinicCounter(clinicId, metric).increment(amount);
  }

  @PostConstruct
  private void init() {
    serviceName = System.getenv().getOrDefault("DD_SERVICE", "health-tracker.rest");
    statsdAgentHost = System.getenv().getOrDefault("DD_AGENT_HOST", "localhost");

    meterRegistry =
        new StatsdMeterRegistry(
            new StatsdConfig() {
              @Override
              public String get(String key) {
                return null;
              }

              @Override
              public StatsdFlavor flavor() {
                return StatsdFlavor.DATADOG;
              }

              @Override
              public String host() {
                return statsdAgentHost;
              }
            },
            Clock.SYSTEM);
  }

  /**
   * Fetch or create a counter for the given metric and clinic
   *
   * @param clinicId
   * @param metric
   * @return
   */
  private Counter getClinicCounter(Long clinicId, HealthTrackerCounterMetric metric) {
    return counterCache.computeIfAbsent(
        new ClinicCounterKey(metric, clinicId),
        k ->
            Counter.builder(metric.getName())
                .description(metric.getDescription())
                .tags("service", serviceName)
                .tags("clinicId", clinicId.toString())
                .register(meterRegistry));
  }
}
