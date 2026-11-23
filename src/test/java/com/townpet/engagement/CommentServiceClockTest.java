package com.townpet.engagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.townpet.publication.api.ActivePublication;
import com.townpet.publication.api.GuestDirectory;
import com.townpet.publication.api.PublicationAccess;
import com.townpet.relationship.api.BlockDirectory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class CommentServiceClockTest {
  private static final UUID MEMBER = UUID.fromString("00000000-0000-4000-8000-000000000201");
  private static final UUID PUBLICATION = UUID.fromString("00000000-0000-4000-8000-000000000301");
  private static final Instant CREATED = Instant.parse("2026-01-02T03:04:05Z");
  private static final Instant EDITED = Instant.parse("2026-01-09T10:11:12Z");

  private final CommentRepository comments = mock(CommentRepository.class);
  private final PublicationAccess publications = mock(PublicationAccess.class);

  @Test
  void stampsCreationAndEditsWithTheInjectedClock() {
    when(publications.findActive(PUBLICATION))
        .thenReturn(Optional.of(new ActivePublication(PUBLICATION, null)));
    when(comments.saveAndFlush(any(CommentEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CommentEntity created = serviceAt(CREATED).create(MEMBER, PUBLICATION, null, "첫 댓글");
    assertEquals(CREATED, created.getCreatedAt());
    assertEquals(CREATED, created.getUpdatedAt());

    when(comments.findByIdAndLifecycle(created.getId(), CommentLifecycle.ACTIVE))
        .thenReturn(Optional.of(created));
    CommentEntity edited =
        serviceAt(EDITED).edit(MEMBER, PUBLICATION, created.getId(), created.getVersion(), "수정");
    assertEquals(CREATED, edited.getCreatedAt());
    assertEquals(EDITED, edited.getUpdatedAt());
  }

  private CommentService serviceAt(Instant now) {
    return new CommentService(
        comments,
        publications,
        mock(BlockDirectory.class),
        mock(GuestDirectory.class),
        mock(ApplicationEventPublisher.class),
        Clock.fixed(now, ZoneOffset.UTC));
  }
}
