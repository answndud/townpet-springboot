package com.townpet.publication;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/posts/{publicationId}")
class PublicationMetricsController {
  private final PublicationService publications;
  private final JdbcTemplate jdbc;

  PublicationMetricsController(PublicationService publications, JdbcTemplate jdbc) {
    this.publications = publications;
    this.jdbc = jdbc;
  }

  @PostMapping("/view")
  @Transactional
  ViewResponse view(@PathVariable UUID publicationId) {
    requireVisible(publicationId);
    Long viewCount =
        jdbc.queryForObject(
            "INSERT INTO publication_metric (publication_id, view_count) VALUES (?, 1) "
                + "ON CONFLICT (publication_id) DO UPDATE SET view_count = "
                + "publication_metric.view_count + 1 RETURNING view_count",
            Long.class,
            publicationId);
    return new ViewResponse(viewCount == null ? 1 : viewCount);
  }

  @GetMapping("/stats")
  @Transactional(readOnly = true)
  ViewResponse stats(@PathVariable UUID publicationId) {
    requireVisible(publicationId);
    return new ViewResponse(
        jdbc
            .query(
                "SELECT view_count FROM publication_metric WHERE publication_id = ?",
                (rs, rowNum) -> rs.getLong("view_count"),
                publicationId)
            .stream()
            .findFirst()
            .orElse(0L));
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
