package com.townpet.publication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.townpet.catalog.api.AnimalCommunityTagger;
import com.townpet.member.api.MemberDirectory;
import com.townpet.publication.api.GuestDirectory;
import com.townpet.relationship.api.BlockDirectory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicationServiceClockTest {
  private static final UUID GUEST_PUBLIC_ID = UUID.fromString("00000000-0000-4000-8000-000000000a01");
  private static final UUID GUEST_INTERNAL_ID =
      UUID.fromString("00000000-0000-4000-8000-000000000a02");
  private static final Instant CREATED = Instant.parse("2026-02-03T04:05:06Z");
  private static final Instant EDITED = Instant.parse("2026-02-10T11:12:13Z");

  private final PublicationRepository publications = mock(PublicationRepository.class);
  private final GuestDirectory guests = mock(GuestDirectory.class);

  @Test
  void stampsGuestPublicationsWithTheInjectedClock() {
    when(guests.authenticate(GUEST_PUBLIC_ID, "password-123"))
        .thenReturn(new GuestDirectory.GuestIdentity(GUEST_INTERNAL_ID, GUEST_PUBLIC_ID));
    when(publications.save(any(PublicationEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(publications.saveAndFlush(any(PublicationEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PublicationEntity created =
        serviceAt(CREATED).createGuest(GUEST_PUBLIC_ID, "password-123", "제목", "본문");
    assertEquals(CREATED, created.getCreatedAt());
    assertEquals(CREATED, created.getUpdatedAt());

    when(publications.findByIdAndLifecycle(created.getId(), PublicationLifecycle.ACTIVE))
        .thenReturn(Optional.of(created));
    PublicationEntity edited =
        serviceAt(EDITED)
            .editGuest(
                GUEST_PUBLIC_ID,
                "password-123",
                created.getId(),
                created.getVersion(),
                "새 제목",
                "새 본문");
    assertEquals(CREATED, edited.getCreatedAt());
    assertEquals(EDITED, edited.getUpdatedAt());
  }

  private PublicationService serviceAt(Instant now) {
    return new PublicationService(
        publications,
        mock(MemberDirectory.class),
        mock(BlockDirectory.class),
        guests,
        mock(AnimalCommunityTagger.class),
        Clock.fixed(now, ZoneOffset.UTC));
  }
}
