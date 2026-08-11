package com.townpet.publication;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/posts/{publicationId}")
class PublicationMetricsController {
  private final PublicationService publications;
  private final PublicationMetricRepository metrics;

  PublicationMetricsController(
      PublicationService publications, PublicationMetricRepository metrics) {
    this.publications = publications;
    this.metrics = metrics;
  }

  @PostMapping("/view")
  @Transactional
  ViewResponse view(@PathVariable UUID publicationId) {
    requireVisible(publicationId);
    PublicationMetricEntity metric =
        metrics
            .findById(publicationId)
            .orElseGet(() -> metrics.save(new PublicationMetricEntity(publicationId)));
    metric.increment();
    return new ViewResponse(metric.getViewCount());
  }

  @GetMapping("/stats")
  @Transactional(readOnly = true)
  ViewResponse stats(@PathVariable UUID publicationId) {
    requireVisible(publicationId);
    return new ViewResponse(
        metrics.findById(publicationId).map(PublicationMetricEntity::getViewCount).orElse(0L));
  }

  @PostMapping("/share")
  ShareResponse share(@PathVariable UUID publicationId) {
    requireVisible(publicationId);
    return new ShareResponse("/posts/" + publicationId);
  }

  record ViewResponse(long viewCount) {}

  record ShareResponse(String path) {}

  private void requireVisible(UUID publicationId) {
    if (publications.findVisible(publicationId, null).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }
}
