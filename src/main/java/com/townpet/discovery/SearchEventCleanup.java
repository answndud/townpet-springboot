package com.townpet.discovery;

import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class SearchEventCleanup {
  private final SearchEventRepository events;
  private final Duration retention;

  SearchEventCleanup(
      SearchEventRepository events,
      @Value("${townpet.security.search-event-retention:30d}") Duration retention) {
    if (retention.isNegative() || retention.isZero()) {
      throw new IllegalArgumentException("Search event retention must be positive");
    }
    this.events = events;
    this.retention = retention;
  }

  @Scheduled(fixedDelayString = "${townpet.security.search-event-cleanup-ms:3600000}")
  @Transactional
  void deleteExpired() {
    events.deleteByCreatedAtBefore(Instant.now().minus(retention));
  }
}
