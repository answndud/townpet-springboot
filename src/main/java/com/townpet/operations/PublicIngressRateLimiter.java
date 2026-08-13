package com.townpet.operations;

import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Bounds anonymous telemetry ingress when the portfolio runs as one application instance. A
 * distributed limiter is intentionally deferred until the deployment topology requires it.
 */
@Component
final class PublicIngressRateLimiter {
  private static final int MAX_REQUESTS_PER_WINDOW = 600;
  private static final Duration WINDOW = Duration.ofMinutes(1);

  private Instant windowStarted = Instant.now();
  private int requests;

  synchronized void requireCapacity() {
    Instant now = Instant.now();
    if (Duration.between(windowStarted, now).compareTo(WINDOW) >= 0) {
      windowStarted = now;
      requests = 0;
    }
    if (requests >= MAX_REQUESTS_PER_WINDOW) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "public telemetry rate limit exceeded");
    }
    requests++;
  }
}
