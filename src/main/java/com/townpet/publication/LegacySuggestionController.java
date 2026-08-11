package com.townpet.publication;

import com.townpet.publication.api.PublicationFeed;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/suggestions")
class LegacySuggestionController {
  private final PublicationFeed feed;

  LegacySuggestionController(PublicationFeed feed) {
    this.feed = feed;
  }

  @GetMapping
  List<PublicationController.PublicationResponse> list() {
    return feed.list(null, false, null, 10).items().stream()
        .map(
            item ->
                new PublicationController.PublicationResponse(
                    item.id(),
                    PublicationType.valueOf(item.type()),
                    PublicationScope.valueOf(item.scope()),
                    item.authorId(),
                    item.neighborhoodId(),
                    item.title(),
                    item.body(),
                    PublicationLifecycle.valueOf(item.lifecycle()),
                    item.createdAt(),
                    item.updatedAt(),
                    item.version()))
        .toList();
  }
}
