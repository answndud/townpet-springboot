package com.townpet.common;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Small bounded fixed-window limiter for a single application instance.
 *
 * <p>This is intentionally not a distributed security boundary. A reverse proxy or shared store is
 * still required when more than one application instance accepts public traffic.
 */
@Component
public final class RequestRateLimiter {
  private static final int MAX_KEYS = 10_000;
  private final Map<String, Window> windows = new LinkedHashMap<>();
  private final MeterRegistry metrics;

  public RequestRateLimiter() {
    this(Metrics.globalRegistry);
  }

  @Autowired
  RequestRateLimiter(MeterRegistry metrics) {
    this.metrics = metrics;
  }

  public synchronized void requireCapacity(
      String bucket, String key, int maxRequests, Duration windowDuration) {
    Instant now = Instant.now();
    String mapKey = bucket + "\u0000" + key;
    Window current = windows.get(mapKey);
    if (current == null
        || Duration.between(current.startedAt(), now).compareTo(windowDuration) >= 0) {
      if (windows.size() >= MAX_KEYS) {
        evictExpired(now, windowDuration);
      }
      if (windows.size() >= MAX_KEYS) {
        Iterator<String> iterator = windows.keySet().iterator();
        if (iterator.hasNext()) {
          windows.remove(iterator.next());
        }
      }
      current = new Window(now, 0);
      windows.put(mapKey, current);
    }
    if (current.count() >= maxRequests) {
      metrics.counter("townpet.security.rate_limit.rejections", "bucket", bucket).increment();
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "request rate limit exceeded");
    }
    windows.put(mapKey, new Window(current.startedAt(), current.count() + 1));
  }

  private void evictExpired(Instant now, Duration windowDuration) {
    windows
        .entrySet()
        .removeIf(
            entry ->
                Duration.between(entry.getValue().startedAt(), now).compareTo(windowDuration) >= 0);
  }

  private record Window(Instant startedAt, int count) {}
}
