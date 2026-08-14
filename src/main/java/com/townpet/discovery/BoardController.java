package com.townpet.discovery;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
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

/** Serves boards that are shared across animal communities. */
@Validated
@RestController
@RequestMapping("/api/v1/boards")
class BoardController {
  private static final Map<String, String> COMMON_BOARD_TYPES =
      Map.ofEntries(
          Map.entry("adoption", "ADOPTION"),
          Map.entry("lost-found", "LOST_FOUND"),
          Map.entry("hospital-reviews", "HOSPITAL_REVIEW"),
          Map.entry("gatherings", "GATHERING"),
          Map.entry("marketplace", "MARKETPLACE"),
          Map.entry("care", "CARE_REQUEST"),
          Map.entry("volunteer", "VOLUNTEER"));
  private static final Set<String> COMMON_BOARD_ITEM_TYPES =
      Set.copyOf(COMMON_BOARD_TYPES.values());

  private final CommunityFeed feed;

  BoardController(CommunityFeed feed) {
    this.feed = feed;
  }

  @GetMapping("/{boardCode}/feed")
  FeedController.FeedResponse list(
      @PathVariable String boardCode,
      @AuthenticationPrincipal @Nullable UserDetails principal,
      @RequestParam(required = false) @Size(max = 512) @Nullable String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
      @RequestParam(required = false) @Size(max = 80) @Nullable String query,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    String normalizedBoard = normalizeBoardCode(boardCode);
    if (from != null && to != null && to.isBefore(from)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feed date range");
    }
    try {
      CommunityFeed.Page page =
          feed.list(
              memberId(principal),
              principal != null,
              cursor,
              limit,
              query,
              from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC),
              to == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
              null,
              "all".equals(normalizedBoard)
                  ? COMMON_BOARD_ITEM_TYPES
                  : Set.of(COMMON_BOARD_TYPES.get(normalizedBoard)));
      return new FeedController.FeedResponse(
          page.items().stream().map(BoardController::toResponse).toList(),
          new FeedController.PageInfo(page.nextCursor(), page.hasNext(), page.totalPages()));
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid common board request");
    }
  }

  private static String normalizeBoardCode(String raw) {
    String code = raw.trim().toLowerCase(java.util.Locale.ROOT);
    if (!code.equals("all") && !COMMON_BOARD_TYPES.containsKey(code)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown common board");
    }
    return code;
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

  private static FeedController.FeedItemResponse toResponse(CommunityFeed.Item item) {
    return new FeedController.FeedItemResponse(
        item.sourceId(),
        item.itemKind(),
        item.itemType(),
        item.title(),
        item.summary(),
        item.authorId(),
        item.neighborhoodId(),
        null,
        item.status(),
        item.status(),
        item.createdAt(),
        item.updatedAt(),
        0L,
        item.targetPath());
  }
}
