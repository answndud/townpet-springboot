package com.townpet.discovery;

import com.townpet.publication.api.PublicationFeed;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/personalization")
@PreAuthorize("hasRole('MODERATOR')")
class AdminPersonalizationController {
  private final PublicationFeed feed;

  AdminPersonalizationController(PublicationFeed feed) {
    this.feed = feed;
  }

  @GetMapping
  Response get() {
    List<Candidate> candidates =
        feed.popular(10).items().stream()
            .map(item -> new Candidate(item.id(), item.title(), item.createdAt()))
            .toList();
    return new Response("PUBLIC_VIEW_COUNT_RECENCY", candidates);
  }

  record Response(String strategy, List<Candidate> candidates) {}

  record Candidate(UUID publicationId, String title, Instant createdAt) {}
}
