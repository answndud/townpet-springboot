package com.townpet.common;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** PostgreSQL-backed fixed-window limiter shared by all application instances. */
@Component
public final class RequestRateLimiter {
  private static final long CLEANUP_INTERVAL_MILLIS = 10 * 60 * 1000L;
  @Nullable private final JdbcTemplate jdbc;
  private final boolean sharedDatabaseEnabled;
  private final Map<String, Window> windows = new LinkedHashMap<>();
  private final MeterRegistry metrics;
  private long lastCleanupMillis;

  /** Test-only fallback for unit tests that do not load the application context. */
  public RequestRateLimiter() {
    this(null, Metrics.globalRegistry);
  }

  @Autowired
  RequestRateLimiter(@Nullable JdbcTemplate jdbc, MeterRegistry metrics) {
    this.jdbc = jdbc;
    this.sharedDatabaseEnabled = jdbc != null && !isH2(jdbc);
    this.metrics = metrics;
  }

  public void requireCapacity(String bucket, String key, int maxRequests, Duration windowDuration) {
    if (sharedDatabaseEnabled) {
      requireDatabaseCapacity(bucket, key, maxRequests, windowDuration);
      return;
    }
    requireInMemoryCapacity(bucket, key, maxRequests, windowDuration);
  }

  private void requireDatabaseCapacity(
      String bucket, String key, int maxRequests, Duration windowDuration) {
    if (maxRequests < 1 || windowDuration.isNegative() || windowDuration.isZero()) {
      throw new IllegalArgumentException("Invalid rate limit policy");
    }
    Instant now = Instant.now();
    Instant windowStartedAt = now.minus(windowDuration);
    cleanupExpiredWindows(windowStartedAt);
    JdbcTemplate database = database();
    try {
      database.queryForObject(
          """
          INSERT INTO security_rate_limit_window(bucket, rate_key, window_started_at, request_count)
          VALUES (?, ?, ?, 1)
          ON CONFLICT (bucket, rate_key) DO UPDATE
          SET window_started_at = CASE
                WHEN security_rate_limit_window.window_started_at <= ?
                THEN ?
                ELSE security_rate_limit_window.window_started_at
              END,
              request_count = CASE
                WHEN security_rate_limit_window.window_started_at <= ?
                THEN 1
                ELSE security_rate_limit_window.request_count + 1
              END
          WHERE security_rate_limit_window.window_started_at <= ?
             OR security_rate_limit_window.request_count < ?
          RETURNING request_count
          """,
          Integer.class,
          bucket,
          key,
          Timestamp.from(now),
          Timestamp.from(windowStartedAt),
          Timestamp.from(now),
          Timestamp.from(windowStartedAt),
          Timestamp.from(windowStartedAt),
          maxRequests);
    } catch (EmptyResultDataAccessException exception) {
      reject(bucket);
    }
  }

  private synchronized void cleanupExpiredWindows(Instant windowStartedAt) {
    long now = System.currentTimeMillis();
    if (now - lastCleanupMillis < CLEANUP_INTERVAL_MILLIS) return;
    database()
        .update(
            "DELETE FROM security_rate_limit_window WHERE window_started_at < ?",
            Timestamp.from(windowStartedAt.minus(Duration.ofHours(1))));
    lastCleanupMillis = now;
  }

  private JdbcTemplate database() {
    if (jdbc == null) throw new IllegalStateException("Database rate limiter is not configured");
    return jdbc;
  }

  private static boolean isH2(JdbcTemplate database) {
    if (database.getDataSource() == null) return false;
    try (var connection = database.getDataSource().getConnection()) {
      return "H2".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
    } catch (SQLException exception) {
      throw new IllegalStateException("Could not inspect rate limiter database", exception);
    }
  }

  private synchronized void requireInMemoryCapacity(
      String bucket, String key, int maxRequests, Duration windowDuration) {
    Instant now = Instant.now();
    String mapKey = bucket + "\u0000" + key;
    Window current = windows.get(mapKey);
    if (current == null
        || Duration.between(current.startedAt(), now).compareTo(windowDuration) >= 0) {
      current = new Window(now, 0);
      windows.put(mapKey, current);
    }
    if (current.count() >= maxRequests) {
      reject(bucket);
    }
    windows.put(mapKey, new Window(current.startedAt(), current.count() + 1));
  }

  private void reject(String bucket) {
    metrics.counter("townpet.security.rate_limit.rejections", "bucket", bucket).increment();
    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "request rate limit exceeded");
  }

  private record Window(Instant startedAt, int count) {}
}
