package com.townpet.discovery;

import com.townpet.publication.api.PublicationFeed;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping({
  "/api/v1/feed",
  "/api/search/guest",
  "/api/home/feed",
  "/api/feed/guest",
  "/api/lounges/breeds/{breedCode}/posts"
})
class FeedController {
  private final PublicationFeed publications;

  FeedController(PublicationFeed publications) {
    this.publications = publications;
  }

  @GetMapping
  FeedResponse list(
      @AuthenticationPrincipal @Nullable UserDetails principal,
      @RequestParam(defaultValue = "VIEWER") FeedAudience audience,
      @RequestParam(required = false) @Size(max = 512) @Nullable String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
      @RequestParam(defaultValue = "ALL") FeedScope scope,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return listFeed(principal, audience, cursor, limit, null, scope, from, to);
  }

  @GetMapping(params = "query")
  FeedResponse list(
      @AuthenticationPrincipal @Nullable UserDetails principal,
      @RequestParam(defaultValue = "VIEWER") FeedAudience audience,
      @RequestParam(required = false) @Size(max = 512) @Nullable String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
      @RequestParam @Size(max = 80) String query,
      @RequestParam(defaultValue = "ALL") FeedScope scope,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return listFeed(principal, audience, cursor, limit, query, scope, from, to);
  }

  private FeedResponse listFeed(
      @AuthenticationPrincipal @Nullable UserDetails principal,
      FeedAudience audience,
      @Nullable String cursor,
      int limit,
      @Nullable String query,
      FeedScope scope,
      @Nullable LocalDate from,
      @Nullable LocalDate to) {
    try {
      PublicationFeed.Page page =
          publications.list(
              memberId(principal),
              audience == FeedAudience.VIEWER,
              cursor,
              limit,
              query,
              scope.name(),
              from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC),
              to == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
      return new FeedResponse(
          page.items().stream().map(FeedController::toResponse).toList(),
          new PageInfo(page.nextCursor(), page.hasNext()));
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feed cursor");
    }
  }

  @Nullable
  private static UUID memberId(@Nullable UserDetails principal) {
    if (principal == null) {
      return null;
    }
    try {
      return UUID.fromString(principal.getUsername());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }

  private static PublicationResponse toResponse(PublicationFeed.Item item) {
    return new PublicationResponse(
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

  record FeedResponse(List<PublicationResponse> items, PageInfo page) {}

  record PageInfo(@Nullable String nextCursor, boolean hasNext) {}

  record PublicationResponse(
      UUID id,
      String type,
      String title,
      String body,
      String scope,
      UUID authorId,
      @Nullable UUID neighborhoodId,
      String lifecycle,
      Instant createdAt,
      Instant updatedAt,
      long version) {}

  enum FeedAudience {
    GLOBAL,
    VIEWER
  }

  enum FeedScope {
    ALL,
    GLOBAL,
    LOCAL
  }
}
