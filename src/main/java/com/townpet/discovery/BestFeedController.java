package com.townpet.discovery;

import com.townpet.publication.api.PublicationFeed;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feed/popular")
class BestFeedController {
  private final PublicationFeed feed;

  BestFeedController(PublicationFeed feed) {
    this.feed = feed;
  }

  @GetMapping
  FeedResponse list() {
    PublicationFeed.Page page = feed.popular(30);
    return new FeedResponse(page.items().stream().map(BestFeedController::response).toList());
  }

  private static Response response(PublicationFeed.Item item) {
    return new Response(item.id(), item.title(), item.body(), item.createdAt());
  }

  record FeedResponse(List<Response> items) {}

  record Response(UUID id, String title, String body, Instant createdAt) {}
}
