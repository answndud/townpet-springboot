package com.townpet.engagement;

import com.townpet.publication.api.PublicationAccess;
import com.townpet.relationship.api.BlockDirectory;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class BookmarkService {
  private final BookmarkRepository bookmarks;
  private final PublicationAccess publications;
  private final BlockDirectory blocks;

  BookmarkService(
      BookmarkRepository bookmarks, PublicationAccess publications, BlockDirectory blocks) {
    this.bookmarks = bookmarks;
    this.publications = publications;
    this.blocks = blocks;
  }

  @Transactional(readOnly = true)
  BookmarkState state(UUID publicationId, @Nullable UUID memberId) {
    requireAccessiblePublication(publicationId, memberId);
    boolean active =
        memberId != null
            && bookmarks.findByPublicationIdAndMemberId(publicationId, memberId).isPresent();
    return new BookmarkState(active);
  }

  @Transactional
  BookmarkState set(UUID memberId, UUID publicationId, boolean active) {
    requireAccessiblePublication(publicationId, memberId);
    var existing = bookmarks.findByPublicationIdAndMemberId(publicationId, memberId);
    if (active && existing.isEmpty()) {
      try {
        bookmarks.saveAndFlush(new BookmarkEntity(publicationId, memberId));
      } catch (DataAccessException exception) {
        throw new BookmarkPublicationNotFoundException();
      }
    } else if (!active) {
      existing.ifPresent(bookmarks::delete);
    }
    return new BookmarkState(active);
  }

  private void requireAccessiblePublication(UUID publicationId, @Nullable UUID viewerMemberId) {
    UUID authorId =
        publications
            .activeAuthorMemberId(publicationId)
            .orElseThrow(BookmarkPublicationNotFoundException::new);
    if (viewerMemberId != null && blocks.isBlocked(viewerMemberId, authorId)) {
      throw new BookmarkPublicationNotFoundException();
    }
  }

  record BookmarkState(boolean active) {}
}

final class BookmarkPublicationNotFoundException extends RuntimeException {}
