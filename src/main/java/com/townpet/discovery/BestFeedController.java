package com.townpet.discovery;

import com.townpet.publication.api.PublicationFeed;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feed/popular")
class BestFeedController {
  private final PublicationFeed feed;

  BestFeedController(PublicationFeed feed) {
    this.feed = feed;
  }

  @GetMapping
  FeedResponse list(
      @RequestParam(required = false) @Nullable String query,
      @RequestParam(defaultValue = "ALL") String searchField,
      @RequestParam(required = false) @Nullable String cursor,
      @RequestParam(defaultValue = "20") int limit) {
    PublicationFeed.PopularPage page = feed.popularPage(limit, cursor, query, searchField);
    return new FeedResponse(
        java.util.stream.IntStream.range(0, page.items().size())
            .mapToObj(index -> response(page.items().get(index), index + 1))
            .toList(),
        new PageInfo(page.nextCursor(), page.hasNext(), page.totalPages()));
  }

  private static Response response(PublicationFeed.PopularItem ranked, int rank) {
    PublicationFeed.Item item = ranked.publication();
    return new Response(
        item.id(), item.title(), item.body(), item.createdAt(), ranked.recommendationCount(), rank);
  }

  record FeedResponse(List<Response> items, PageInfo page) {}

  record PageInfo(@Nullable String nextCursor, boolean hasNext, int totalPages) {}

  record Response(
      UUID id, String title, String body, Instant createdAt, long recommendationCount, int rank) {}
}
