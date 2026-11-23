package com.townpet.discovery;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class SearchEventCleanup {
  private static final Logger log = LoggerFactory.getLogger(SearchEventCleanup.class);
  private final SearchEventRepository events;
  private final Duration retention;
  private final Clock clock;

  @Autowired
  SearchEventCleanup(
      SearchEventRepository events,
      @Value("${townpet.security.search-event-retention:30d}") Duration retention,
      Clock clock) {
    if (retention.isNegative() || retention.isZero()) {
      throw new IllegalArgumentException("Search event retention must be positive");
    }
    this.events = events;
    this.retention = retention;
    this.clock = clock;
  }

  /** Uses the system clock; kept for callers that do not need to control time. */
  SearchEventCleanup(SearchEventRepository events, Duration retention) {
    this(events, retention, Clock.systemUTC());
  }

  @Scheduled(fixedDelayString = "${townpet.security.search-event-cleanup-ms:3600000}")
  @Transactional
  void deleteExpired() {
    Instant startedAt = Instant.now(clock);
    try {
      long deleted = events.deleteByCreatedAtBefore(startedAt.minus(retention));
      log.info(
          "event=scheduled_job job=search_event_cleanup outcome=success deleted={} duration_ms={}",
          deleted,
          Duration.between(startedAt, Instant.now(clock)).toMillis());
    } catch (RuntimeException exception) {
      log.error(
          "event=scheduled_job job=search_event_cleanup outcome=failure duration_ms={}",
          Duration.between(startedAt, Instant.now(clock)).toMillis(),
          exception);
      throw exception;
    }
  }
}
