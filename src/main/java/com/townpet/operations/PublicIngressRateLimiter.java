package com.townpet.operations;

import com.townpet.common.ClientAddress;
import com.townpet.common.RequestRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** Bounds anonymous ingress with counters shared through the configured database. */
@Component
public final class PublicIngressRateLimiter {
  private final RequestRateLimiter limiter;
  private final ClientAddress clientAddress;

  public PublicIngressRateLimiter(RequestRateLimiter limiter, ClientAddress clientAddress) {
    this.limiter = limiter;
    this.clientAddress = clientAddress;
  }

  public void requireCapacity(HttpServletRequest request) {
    limiter.requireCapacity(
        "public-telemetry", clientAddress.resolve(request), 600, Duration.ofMinutes(1));
  }

  public void requireSearchCapacity(HttpServletRequest request) {
    limiter.requireCapacity(
        "public-search", clientAddress.resolve(request), 120, Duration.ofMinutes(1));
    limiter.requireCapacity("public-search-global", "all", 10_000, Duration.ofHours(1));
  }
}
