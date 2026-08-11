package com.townpet.localguide;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/local-resources")
class LocalResourceController {
  private final LocalResourceRepository resources;

  LocalResourceController(LocalResourceRepository resources) { this.resources = resources; }

  @GetMapping
  List<ResourceResponse> list(
      @RequestParam(required = false) LocalResourceKind kind,
      @RequestParam(defaultValue = "") String query) {
    if (query.length() > 80) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is too long");
    return resources.search(kind, query.trim()).stream().map(LocalResourceController::toResponse).toList();
  }

  @GetMapping("/{resourceId}")
  ResourceResponse get(@PathVariable UUID resourceId) {
    return resources.findById(resourceId).map(LocalResourceController::toResponse)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private static ResourceResponse toResponse(LocalResourceEntity resource) {
    return new ResourceResponse(resource.getId(), resource.getKind(), resource.getTitle(), resource.getSummary(), resource.getContent(), resource.getSourceName(), resource.getSourceUrl(), resource.getUpdatedAt());
  }

  record ResourceResponse(UUID id, LocalResourceKind kind, String title, String summary, String content, String sourceName, String sourceUrl, Instant updatedAt) {}
}
