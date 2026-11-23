package com.townpet.engagement;

import com.townpet.common.CurrentMember;
import com.townpet.common.MemberOnly;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members/me/bookmarks")
class BookmarkListController {
  private final BookmarkService bookmarks;

  BookmarkListController(BookmarkService bookmarks) {
    this.bookmarks = bookmarks;
  }

  @GetMapping
  @MemberOnly
  List<UUID> list(@CurrentMember UUID memberId) {
    return bookmarks.list(memberId);
  }
}
