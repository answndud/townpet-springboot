package com.townpet.discovery;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
  "/api/lounges/breeds/{breedCode}/posts"
})
class FeedController {
  private final CommunityFeed feed;

  FeedController(CommunityFeed feed) {
    this.feed = feed;
  }

  @GetMapping
  FeedResponse list(
      @AuthenticationPrincipal @Nullable UserDetails principal,
      @RequestParam(defaultValue = "VIEWER") FeedAudience audience,
      @RequestParam(required = false) @Size(max = 512) @Nullable String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
      @RequestParam(defaultValue = "ALL") @Size(max = 10) String searchField,
      @RequestParam(defaultValue = "ALL") FeedScope scope,
      @RequestParam(required = false) @Size(max = 512) @Nullable String animals,
      @RequestParam(required = false) @Size(max = 40) @Nullable String type,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return listFeed(
        principal, audience, cursor, limit, null, searchField, scope, animals, type, from, to);
  }

  @GetMapping(params = "query")
  FeedResponse list(
      @AuthenticationPrincipal @Nullable UserDetails principal,
      @RequestParam(defaultValue = "VIEWER") FeedAudience audience,
      @RequestParam(required = false) @Size(max = 512) @Nullable String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
      @RequestParam @Size(max = 80) String query,
      @RequestParam(defaultValue = "ALL") @Size(max = 10) String searchField,
      @RequestParam(defaultValue = "ALL") FeedScope scope,
      @RequestParam(required = false) @Size(max = 512) @Nullable String animals,
      @RequestParam(required = false) @Size(max = 40) @Nullable String type,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return listFeed(
        principal, audience, cursor, limit, query, searchField, scope, animals, type, from, to);
  }

  private FeedResponse listFeed(
      @AuthenticationPrincipal @Nullable UserDetails principal,
      FeedAudience audience,
      @Nullable String cursor,
      int limit,
      @Nullable String query,
      String searchField,
      FeedScope scope,
      @Nullable String animals,
      @Nullable String type,
      @Nullable LocalDate from,
      @Nullable LocalDate to) {
    if (from != null && to != null && to.isBefore(from)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feed date range");
    }
    try {
      CommunityFeed.Page page =
          feed.list(
              memberId(principal),
              audience == FeedAudience.VIEWER,
              cursor,
              limit,
              query,
              searchField,
              scope.name(),
              from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC),
              to == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
              parseAnimals(animals),
              parseTypes(type));
      return new FeedResponse(
          page.items().stream().map(FeedController::toResponse).toList(),
          new PageInfo(page.nextCursor(), page.hasNext(), page.totalPages()));
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

  private static FeedItemResponse toResponse(CommunityFeed.Item item) {
    return new FeedItemResponse(
        item.sourceId(),
        item.itemKind(),
        item.itemType(),
        item.title(),
        item.summary(),
        item.scope(),
        item.authorId(),
        item.neighborhoodId(),
        item.animalInterestCode(),
        item.status(),
        item.status(),
        item.createdAt(),
        item.updatedAt(),
        0L,
        item.targetPath());
  }

  record FeedResponse(List<FeedItemResponse> items, PageInfo page) {}

  record PageInfo(@Nullable String nextCursor, boolean hasNext, int totalPages) {}

  record FeedItemResponse(
      UUID id,
      String kind,
      String type,
      String title,
      String body,
      String scope,
      @Nullable UUID authorId,
      @Nullable UUID neighborhoodId,
      @Nullable String animalInterestCode,
      String status,
      String lifecycle,
      Instant createdAt,
      Instant updatedAt,
      long version,
      @Nullable String href) {
    FeedItemResponse(
        UUID id,
        String kind,
        String type,
        String title,
        String body,
        String scope,
        @Nullable UUID authorId,
        @Nullable UUID neighborhoodId,
        String lifecycle,
        Instant createdAt,
        Instant updatedAt,
        long version) {
      this(
          id,
          kind,
          type,
          title,
          body,
          scope,
          authorId,
          neighborhoodId,
          null,
          lifecycle,
          lifecycle,
          createdAt,
          updatedAt,
          version,
          null);
    }
  }

  enum FeedAudience {
    GLOBAL,
    VIEWER
  }

  enum FeedScope {
    ALL,
    GLOBAL,
    LOCAL
  }

  @Nullable
  private static Set<String> parseAnimals(@Nullable String raw) {
    if (raw == null) return null;
    if (raw.isBlank()) return Set.of();
    return java.util.Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .peek(
            value -> {
              if (!value.matches("[A-Z0-9_]{1,40}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid animal filter");
              }
            })
        .collect(Collectors.toUnmodifiableSet());
  }

  @Nullable
  private static Set<String> parseTypes(@Nullable String raw) {
    if (raw == null || raw.isBlank()) return null;
    if (!raw.matches("[A-Z0-9_]{1,40}")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid publication type filter");
    }
    return Set.of(raw);
  }
}
