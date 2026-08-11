package com.townpet.discovery;

import com.townpet.member.api.MemberDirectory;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class LegacyDiscoveryCompatibilityController {
  private final com.townpet.publication.api.PublicationFeed feed;
  private final MemberDirectory members;

  LegacyDiscoveryCompatibilityController(
      com.townpet.publication.api.PublicationFeed feed, MemberDirectory members) {
    this.feed = feed;
    this.members = members;
  }

  @PostMapping("/api/feed/personalization")
  FeedController.FeedResponse personalized(
      @AuthenticationPrincipal @Nullable UserDetails principal,
      @RequestBody(required = false) Object ignored) {
    UUID memberId = memberId(principal);
    com.townpet.publication.api.PublicationFeed.Page page =
        feed.list(memberId, memberId != null, null, 20);
    return new FeedController.FeedResponse(
        page.items().stream().map(LegacyDiscoveryCompatibilityController::response).toList(),
        new FeedController.PageInfo(page.nextCursor(), page.hasNext()));
  }

  @GetMapping("/api/profile/audience-segments")
  AudienceResponse segments(@AuthenticationPrincipal UserDetails principal) {
    UUID memberId = requiredMemberId(principal);
    List<String> segments = new java.util.ArrayList<>(List.of("PUBLIC"));
    if (members
            .findPublicationContext(memberId)
            .map(MemberDirectory.MemberPublicationContext::neighborhoodId)
            .orElse(null)
        != null) {
      segments.add("LOCAL");
    }
    return new AudienceResponse(List.copyOf(segments));
  }

  @PostMapping("/api/lounges/breeds/{breedCode}/groupbuys")
  List<Object> groupBuys() {
    return List.of();
  }

  @PostMapping("/api/guest/step-up")
  ResponseEntity<Void> stepUp(@RequestBody(required = false) Object ignored) {
    return ResponseEntity.noContent().build();
  }

  record AudienceResponse(List<String> segments) {}

  private static FeedController.PublicationResponse response(
      com.townpet.publication.api.PublicationFeed.Item item) {
    return new FeedController.PublicationResponse(
        item.id(),
        item.type(),
        item.title(),
        item.body(),
        item.scope(),
        item.authorId(),
        item.neighborhoodId(),
        item.lifecycle(),
        item.createdAt(),
        item.updatedAt(),
        item.version());
  }

  @Nullable
  private static UUID memberId(@Nullable UserDetails principal) {
    if (principal == null) return null;
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private static UUID requiredMemberId(UserDetails principal) {
    UUID memberId = memberId((UserDetails) principal);
    if (memberId == null)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNAUTHORIZED);
    return memberId;
  }
}
