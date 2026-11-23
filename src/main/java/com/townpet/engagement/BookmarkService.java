package com.townpet.engagement;

import com.townpet.common.DomainProblemException;
import com.townpet.publication.api.PublicationAccess;
import com.townpet.relationship.api.BlockDirectory;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class BookmarkService {
  private final BookmarkRepository bookmarks;
  private final PublicationAccess publications;
  private final BlockDirectory blocks;
  private final Clock clock;

  BookmarkService(
      BookmarkRepository bookmarks, PublicationAccess publications, BlockDirectory blocks,
      Clock clock) {
    this.bookmarks = bookmarks;
    this.publications = publications;
    this.blocks = blocks;
    this.clock = clock;
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
        bookmarks.saveAndFlush(new BookmarkEntity(publicationId, memberId, Instant.now(clock)));
      } catch (DataIntegrityViolationException exception) {
        throw new BookmarkPublicationNotFoundException();
      }
    } else if (!active) {
      existing.ifPresent(bookmarks::delete);
    }
    return new BookmarkState(active);
  }

  @Transactional(readOnly = true)
  List<UUID> list(UUID memberId) {
    return bookmarks.findTop100ByMemberIdOrderByCreatedAtDescIdDesc(memberId).stream()
        .map(BookmarkEntity::getPublicationId)
        .toList();
  }

  private void requireAccessiblePublication(UUID publicationId, @Nullable UUID viewerMemberId) {
    UUID authorId =
        publications.findActive(publicationId).orElseThrow(BookmarkPublicationNotFoundException::new).authorMemberId();
    if (viewerMemberId != null
        && authorId != null
        && blocks.isBlocked(viewerMemberId, authorId)) {
      throw new BookmarkPublicationNotFoundException();
    }
  }

  record BookmarkState(boolean active) {}
}

final class BookmarkPublicationNotFoundException extends DomainProblemException {
  BookmarkPublicationNotFoundException() {
    super(HttpStatus.NOT_FOUND, "BOOKMARK_PUBLICATION_NOT_FOUND");
  }
}
