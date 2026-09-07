package com.townpet.operations;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "townpet.events.recovery", name = "enabled", havingValue = "true")
class EventPublicationRecovery {
  private final IncompleteEventPublications publications;
  private final JdbcTemplate jdbc;
  private final Duration minimumAge;
  private final int maxInFlight;
  private final int batchSize;
  private final int maxAttempts;
  private final AtomicBoolean running = new AtomicBoolean();
  private final AtomicInteger backlog = new AtomicInteger();
  private final AtomicLong oldestAgeSeconds = new AtomicLong();

  EventPublicationRecovery(
      IncompleteEventPublications publications,
      JdbcTemplate jdbc,
      MeterRegistry metrics,
      @Value("${townpet.events.recovery.min-age:PT5M}") Duration minimumAge,
      @Value("${townpet.events.recovery.max-in-flight:10}") int maxInFlight,
      @Value("${townpet.events.recovery.batch-size:10}") int batchSize,
      @Value("${townpet.events.recovery.max-attempts:3}") int maxAttempts) {
    this.publications = publications;
    this.jdbc = jdbc;
    this.minimumAge = minimumAge;
    this.maxInFlight = maxInFlight;
    this.batchSize = batchSize;
    this.maxAttempts = maxAttempts;
    Gauge.builder("townpet.events.backlog", backlog, AtomicInteger::get)
        .description("Incomplete event publications awaiting recovery")
        .register(metrics);
    Gauge.builder("townpet.events.oldest_age_seconds", oldestAgeSeconds, AtomicLong::get)
        .description("Age of the oldest incomplete event publication")
        .register(metrics);
  }

  @Scheduled(
      fixedDelayString = "${townpet.events.recovery.fixed-delay-ms:60000}",
      initialDelayString = "${townpet.events.recovery.initial-delay-ms:60000}")
  void recover() {
    if (!running.compareAndSet(false, true)) return;
    try {
      refreshBacklogMetrics();
      publications.resubmitIncompletePublications(
          ResubmissionOptions.defaults()
              .withMinAge(minimumAge)
              .withMaxInFlight(maxInFlight)
              .withBatchSize(batchSize)
              .withFilter(publication -> publication.getCompletionAttempts() < maxAttempts));
      refreshBacklogMetrics();
    } finally {
      running.set(false);
    }
  }

  private void refreshBacklogMetrics() {
    Integer count =
        jdbc.queryForObject(
            "select count(*) from event_publication where completion_date is null and completion_attempts < ?",
            Integer.class,
            maxAttempts);
    Long ageSeconds =
        jdbc.queryForObject(
            """
            select coalesce(extract(epoch from (current_timestamp - min(publication_date))), 0)::bigint
            from event_publication
            where completion_date is null
              and completion_attempts < ?
            """,
            Long.class,
            maxAttempts);
    backlog.set(count == null ? 0 : count);
    oldestAgeSeconds.set(ageSeconds == null ? 0 : Math.max(0, ageSeconds));
  }
}
