package com.townpet.engagement;

import com.townpet.common.MemberOnly;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/members/me/bookmarks")
class BookmarkListController {
  private final BookmarkService bookmarks;

  BookmarkListController(BookmarkService bookmarks) {
    this.bookmarks = bookmarks;
  }

  @GetMapping
  @MemberOnly
  List<UUID> list(@AuthenticationPrincipal UserDetails principal) {
    try {
      return bookmarks.list(UUID.fromString(principal.getUsername()));
    } catch (Exception exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }
}
