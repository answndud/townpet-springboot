package com.townpet.engagement;

import com.townpet.common.CurrentMember;
import com.townpet.common.MemberOnly;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
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
      @PathVariable UUID publicationId, @CurrentMember(required = false) @Nullable UUID currentMemberId) {
    try {
      return toResponse(bookmarks.state(publicationId, currentMemberId));
    } catch (BookmarkPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  @PutMapping
  @MemberOnly
  BookmarkResponse set(
      @CurrentMember UUID currentMemberId,
      @PathVariable UUID publicationId,
      @Valid @RequestBody SetBookmarkRequest request) {
    try {
      return toResponse(
          bookmarks.set(currentMemberId, publicationId, request.active()));
    } catch (BookmarkPublicationNotFoundException exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  private static BookmarkResponse toResponse(BookmarkService.BookmarkState state) {
    return new BookmarkResponse(state.active());
  }

  record SetBookmarkRequest(@NotNull Boolean active) {}

  record BookmarkResponse(boolean active) {}
}
