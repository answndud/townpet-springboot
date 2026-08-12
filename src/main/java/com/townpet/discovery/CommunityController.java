package com.townpet.discovery;

import com.townpet.catalog.api.AnimalInterestCatalog;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/v1/communities")
class CommunityController {
  private static final Map<String, String> BOARD_TYPES =
      Map.ofEntries(
          Map.entry("free", "FREE_BOARD"),
          Map.entry("questions", "QA_QUESTION"),
          Map.entry("adoption", "ADOPTION"),
          Map.entry("lost-found", "LOST_FOUND"),
          Map.entry("hospital-reviews", "HOSPITAL_REVIEW"),
          Map.entry("gatherings", "GATHERING"),
          Map.entry("marketplace", "MARKETPLACE"),
          Map.entry("care", "CARE_REQUEST"),
          Map.entry("volunteer", "VOLUNTEER"),
          Map.entry("showcase", "PET_SHOWCASE"),
          Map.entry("product-reviews", "PRODUCT_REVIEW"));

  private final CommunityFeed feed;

  CommunityController(CommunityFeed feed) {
    this.feed = feed;
  }

  @GetMapping("/{animalCode}/feed")
  CommunityFeedResponse list(
      @PathVariable String animalCode,
      @AuthenticationPrincipal @Nullable UserDetails principal,
      @RequestParam(defaultValue = "VIEWER") FeedController.FeedAudience audience,
      @RequestParam(defaultValue = "all") String board,
      @RequestParam(required = false) @Size(max = 512) @Nullable String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
      @RequestParam(defaultValue = "ALL") FeedController.FeedScope scope,
      @RequestParam(required = false) @Size(max = 80) @Nullable String query,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    String normalizedAnimal = normalizeAnimalCode(animalCode);
    String normalizedBoard = normalizeBoard(board);
    if (from != null && to != null && to.isBefore(from)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feed date range");
    }
    try {
      CommunityFeed.Page page =
          "all".equals(normalizedAnimal)
              ? feed.list(
                  memberId(principal),
                  audience == FeedController.FeedAudience.VIEWER,
                  cursor,
                  limit,
                  query,
                  scope.name(),
                  from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC),
                  to == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                  null,
                  boardType(normalizedBoard))
              : feed.listCommunity(
                  memberId(principal),
                  audience == FeedController.FeedAudience.VIEWER,
                  normalizedAnimal.toUpperCase(java.util.Locale.ROOT),
                  boardType(normalizedBoard),
                  cursor,
                  limit,
                  query,
                  scope.name(),
                  from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC),
                  to == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
      return new CommunityFeedResponse(
          page.items().stream().map(CommunityController::toResponse).toList(),
          new PageInfo(page.nextCursor(), page.hasNext()),
          normalizedAnimal,
          normalizedBoard);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid community feed request");
    }
  }

  private static String normalizeAnimalCode(String raw) {
    String code = raw.trim().toUpperCase(java.util.Locale.ROOT);
    if (!code.equals("ALL") && !AnimalInterestCatalog.codes().contains(code)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown animal community");
    }
    return code.toLowerCase(java.util.Locale.ROOT);
  }

  private static String normalizeBoard(String raw) {
    String board = raw.trim().toLowerCase(java.util.Locale.ROOT);
    if (!board.equals("all") && !BOARD_TYPES.containsKey(board)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown community board");
    }
    return board;
  }

  @Nullable
  private static String boardType(String board) {
    return "all".equals(board) ? null : BOARD_TYPES.get(board);
  }

  @Nullable
  private static UUID memberId(@Nullable UserDetails principal) {
    if (principal == null) return null;
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

  record CommunityFeedResponse(
      List<FeedItemResponse> items, PageInfo page, String animalCode, String board) {}

  record PageInfo(@Nullable String nextCursor, boolean hasNext) {}

  record FeedItemResponse(
      UUID id,
      String kind,
      String type,
      String title,
      String body,
      String scope,
      @Nullable UUID authorId,
      @Nullable UUID neighborhoodId,
      @Nullable String animalCode,
      String status,
      String lifecycle,
      Instant createdAt,
      Instant updatedAt,
      long version,
      @Nullable String href) {}
}
