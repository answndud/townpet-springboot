package com.townpet.engagement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/publications/{publicationId}/bookmark")
class BookmarkController {
  private final BookmarkService bookmarks;

  BookmarkController(BookmarkService bookmarks) {
    this.bookmarks = bookmarks;
  }

  @GetMapping
  BookmarkResponse get(
      @PathVariable UUID publicationId, @AuthenticationPrincipal @Nullable UserDetails principal) {
    try {
      return toResponse(bookmarks.state(publicationId, memberId(principal)));
    } catch (BookmarkPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  @PutMapping
  BookmarkResponse set(
      @AuthenticationPrincipal UserDetails principal,
      @PathVariable UUID publicationId,
      @Valid @RequestBody SetBookmarkRequest request) {
    try {
      return toResponse(
          bookmarks.set(authenticatedMemberId(principal), publicationId, request.active()));
    } catch (BookmarkPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
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

  private static UUID authenticatedMemberId(UserDetails principal) {
    UUID memberId = memberId(principal);
    if (memberId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    return memberId;
  }

  private static BookmarkResponse toResponse(BookmarkService.BookmarkState state) {
    return new BookmarkResponse(state.active());
  }

  record SetBookmarkRequest(@NotNull Boolean active) {}

  record BookmarkResponse(boolean active) {}
}
