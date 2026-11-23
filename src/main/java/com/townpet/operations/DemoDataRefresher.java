package com.townpet.operations;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the synthetic portfolio activities in the future.
 *
 * <p>The demo gatherings, care requests and volunteer opportunities are loaded from the fixtures
 * under {@code migration/fixtures} with fixed ids. Their start times pass as the demo ages, which
 * used to need a new Flyway migration each time. This job moves a row back to its original offset
 * once it starts within a day (or has started), so the sandbox always has upcoming activity.
 *
 * <p>Only the fixture ids below are touched, so it does nothing on a database without them. It
 * reads and writes the tables of other modules directly because it maintains fixture data rather
 * than business state. Disable it with {@code townpet.demo-data.refresh.enabled=false}.
 */
@Component
@ConditionalOnProperty(
    name = "townpet.demo-data.refresh.enabled",
    havingValue = "true",
    matchIfMissing = true)
class DemoDataRefresher {
  private static final Logger log = LoggerFactory.getLogger(DemoDataRefresher.class);
  private static final Duration RESET_WHEN_STARTING_WITHIN = Duration.ofHours(24);
  private static final Duration SHOWN_AS_POSTED = Duration.ofDays(2);

  /** A fixture row and how far ahead of "now" it should start (and end, for care requests). */
  record Slot(String id, Duration startsAfter, Duration endsAfter) {
    static Slot of(String id, Duration startsAfter) {
      return new Slot(id, startsAfter, Duration.ZERO);
    }
  }

  static final List<Slot> GATHERINGS =
      List.of(
          Slot.of("0198f342-13d7-7000-8000-000000000401", days(6)),
          Slot.of("0198f342-13d7-7000-8000-000000000402", days(12)),
          Slot.of("00000000-0000-4000-8000-000000000401", days(6)),
          Slot.of("00000000-0000-4000-8000-000000000402", days(12)),
          Slot.of("00000000-0000-4000-8000-000000009501", days(5)),
          Slot.of("00000000-0000-4000-8000-000000009502", days(9)),
          Slot.of("00000000-0000-4000-8000-000000009503", days(14)),
          Slot.of("00000000-0000-4000-8000-00000000d201", days(6)),
          Slot.of("00000000-0000-4000-8000-00000000d202", days(12)));

  static final List<Slot> CARE_REQUESTS =
      List.of(
          new Slot("00000000-0000-4000-8000-000000009601", days(7), days(9)),
          new Slot("00000000-0000-4000-8000-000000009602", days(14), days(15)),
          new Slot(
              "00000000-0000-4000-8000-000000009603", days(4), days(4).plus(Duration.ofHours(2))),
          new Slot(
              "00000000-0000-4000-8000-00000000f401", days(5), days(5).plus(Duration.ofHours(8))),
          new Slot(
              "00000000-0000-4000-8000-00000000f402", days(10), days(10).plus(Duration.ofHours(1))));

  static final List<Slot> VOLUNTEER_OPPORTUNITIES =
      List.of(
          Slot.of("00000000-0000-4000-8000-000000009701", days(10)),
          Slot.of("00000000-0000-4000-8000-000000009702", days(14)),
          Slot.of("00000000-0000-4000-8000-000000009703", days(21)),
          Slot.of("00000000-0000-4000-8000-00000000f201", days(8)),
          Slot.of("00000000-0000-4000-8000-00000000f202", days(15)));

  private final JdbcTemplate jdbc;
  private final Clock clock;

  DemoDataRefresher(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  @EventListener(ApplicationReadyEvent.class)
  void refreshOnStartup() {
    refreshQuietly();
  }

  @Scheduled(
      fixedDelayString = "${townpet.demo-data.refresh.interval-ms:86400000}",
      initialDelayString = "${townpet.demo-data.refresh.interval-ms:86400000}")
  void refreshDaily() {
    refreshQuietly();
  }

  /** Refreshing sample dates is cosmetic, so a failure must never stop the application. */
  private void refreshQuietly() {
    try {
      refresh();
    } catch (RuntimeException exception) {
      log.warn(
          "event=demo_data_refresh outcome=failure reason={}", exception.getClass().getSimpleName());
    }
  }

  /** Returns how many fixture rows were moved forward. */
  int refresh() {
    Instant now = Instant.now(clock);
    Timestamp resetBefore = Timestamp.from(now.plus(RESET_WHEN_STARTING_WITHIN));
    Timestamp posted = Timestamp.from(now.minus(SHOWN_AS_POSTED));
    int moved = 0;
    for (Slot slot : GATHERINGS) {
      moved +=
          jdbc.update(
              "UPDATE gathering SET starts_at = ?, created_at = ? WHERE id = ? AND starts_at <= ?",
              Timestamp.from(now.plus(slot.startsAfter())),
              posted,
              UUID.fromString(slot.id()),
              resetBefore);
    }
    for (Slot slot : CARE_REQUESTS) {
      moved +=
          jdbc.update(
              "UPDATE care_request SET starts_at = ?, ends_at = ?, created_at = ?, updated_at = ?"
                  + " WHERE id = ? AND starts_at <= ?",
              Timestamp.from(now.plus(slot.startsAfter())),
              Timestamp.from(now.plus(slot.endsAfter())),
              posted,
              posted,
              UUID.fromString(slot.id()),
              resetBefore);
    }
    for (Slot slot : VOLUNTEER_OPPORTUNITIES) {
      moved +=
          jdbc.update(
              "UPDATE volunteer_opportunity SET starts_at = ?, created_at = ?, updated_at = ?"
                  + " WHERE id = ? AND starts_at <= ?",
              Timestamp.from(now.plus(slot.startsAfter())),
              posted,
              posted,
              UUID.fromString(slot.id()),
              resetBefore);
    }
    if (moved > 0) {
      log.info("event=demo_data_refresh outcome=success rows={}", moved);
    }
    return moved;
  }

  private static Duration days(long days) {
    return Duration.ofDays(days);
  }
}
