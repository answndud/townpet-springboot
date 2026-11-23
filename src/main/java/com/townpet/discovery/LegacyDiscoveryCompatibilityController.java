package com.townpet.discovery;

import com.townpet.common.CurrentMember;
import com.townpet.common.MemberOnly;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class LegacyDiscoveryCompatibilityController {
  private final com.townpet.publication.api.PublicationFeed feed;

  LegacyDiscoveryCompatibilityController(com.townpet.publication.api.PublicationFeed feed) {
    this.feed = feed;
  }

  @PostMapping("/api/feed/personalization")
  @MemberOnly
  FeedController.FeedResponse personalized(
      @CurrentMember(required = false) @Nullable UUID currentMemberId,
      @RequestBody(required = false) Object ignored) {
    UUID memberId = currentMemberId;
    com.townpet.publication.api.PublicationFeed.Page page =
        feed.list(memberId, memberId != null, null, 20);
    return new FeedController.FeedResponse(
        page.items().stream().map(LegacyDiscoveryCompatibilityController::response).toList(),
        new FeedController.PageInfo(page.nextCursor(), page.hasNext()));
  }

  private static FeedController.FeedItemResponse response(
      com.townpet.publication.api.PublicationFeed.Item item) {
    return new FeedController.FeedItemResponse(
        item.id(),
        "PUBLICATION",
        item.type(),
        item.title(),
        item.body(),
        item.authorId(),
        null,
        item.lifecycle(),
        item.createdAt(),
        item.updatedAt(),
        item.version());
  }

}
