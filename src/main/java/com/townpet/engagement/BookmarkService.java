package com.townpet.engagement;

import com.townpet.publication.api.PublicationAccess;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class BookmarkService {
  private final BookmarkRepository bookmarks;
  private final PublicationAccess publications;

  BookmarkService(BookmarkRepository bookmarks, PublicationAccess publications) {
    this.bookmarks = bookmarks;
    this.publications = publications;
  }

  @Transactional(readOnly = true)
  BookmarkState state(UUID publicationId, @Nullable UUID memberId) {
    requireActivePublication(publicationId);
    boolean active =
        memberId != null
            && bookmarks.findByPublicationIdAndMemberId(publicationId, memberId).isPresent();
    return new BookmarkState(active);
  }

  @Transactional
  BookmarkState set(UUID memberId, UUID publicationId, boolean active) {
    requireActivePublication(publicationId);
    var existing = bookmarks.findByPublicationIdAndMemberId(publicationId, memberId);
    if (active && existing.isEmpty()) {
      bookmarks.save(new BookmarkEntity(publicationId, memberId));
    } else if (!active) {
      existing.ifPresent(bookmarks::delete);
    }
    return new BookmarkState(active);
  }

  private void requireActivePublication(UUID publicationId) {
    if (!publications.existsActive(publicationId)) {
      throw new BookmarkPublicationNotFoundException();
    }
  }

  record BookmarkState(boolean active) {}
}

final class BookmarkPublicationNotFoundException extends RuntimeException {}
