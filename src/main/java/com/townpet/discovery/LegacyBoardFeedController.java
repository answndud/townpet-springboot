package com.townpet.discovery;

import com.townpet.publication.api.PublicationFeed;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
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
@RequestMapping("/api/boards/{board}/posts")
class LegacyBoardFeedController {
  private final PublicationFeed publications;

  LegacyBoardFeedController(PublicationFeed publications) {
    this.publications = publications;
  }

  @GetMapping
  LegacyList list(
      @PathVariable @Size(min = 1, max = 40) String board,
      @AuthenticationPrincipal @Nullable UserDetails principal,
      @RequestParam(defaultValue = "VIEWER") FeedController.FeedAudience audience,
      @RequestParam(required = false) @Size(max = 512) @Nullable String cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
    if (board.equalsIgnoreCase("adoption")) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Use adoption listing contract");
    }
    try {
      PublicationFeed.Page page =
          publications.list(
              memberId(principal), audience == FeedController.FeedAudience.VIEWER, cursor, limit);
      return new LegacyList(page.items(), page.nextCursor(), page.hasNext());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feed cursor");
    }
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

  record LegacyList(
      List<PublicationFeed.Item> items, @Nullable String nextCursor, boolean hasNext) {}
}
