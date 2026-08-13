package com.townpet.operations;

import com.townpet.common.UuidV7;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
class WebVitalMetricController {
  private final WebVitalMetricRepository metrics;
  private final PublicIngressRateLimiter rateLimiter;

  WebVitalMetricController(WebVitalMetricRepository metrics, PublicIngressRateLimiter rateLimiter) {
    this.metrics = metrics;
    this.rateLimiter = rateLimiter;
  }

  @PostMapping({"/api/v1/operations/web-vitals", "/api/metrics/web-vitals"})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void record(@Valid @RequestBody RecordRequest request) {
    rateLimiter.requireCapacity();
    metrics.save(
        new WebVitalMetricEntity(
            UuidV7.randomUuid(), request.metricName(), request.metricValue(), request.route()));
  }

  @GetMapping({"/api/v1/operations/web-vitals/summary", "/api/ops/web-vitals/summary"})
  @PreAuthorize("hasRole('MODERATOR')")
  Summary summary() {
    List<WebVitalMetricEntity> values = metrics.findTop1000ByOrderByObservedAtDesc();
    if (values.isEmpty()) return new Summary(0, 0, 0);
    double total = values.stream().mapToDouble(WebVitalMetricEntity::getMetricValue).sum();
    return new Summary(
        values.size(),
        total / values.size(),
        values.stream().mapToDouble(WebVitalMetricEntity::getMetricValue).max().orElse(0));
  }

  record RecordRequest(
      @NotBlank @Pattern(regexp = "LCP|CLS|INP|FCP|TTFB") String metricName,
      @DecimalMin("0.0") @DecimalMax("600000.0") double metricValue,
      @NotBlank @Size(max = 200) String route) {}

  record Summary(long count, double average, double max) {}
}
