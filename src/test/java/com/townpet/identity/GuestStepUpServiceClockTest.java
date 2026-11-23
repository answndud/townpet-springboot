package com.townpet.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class GuestStepUpServiceClockTest {
  private static final Instant T0 = Instant.parse("2026-03-01T12:00:00Z");
  private static final String SCOPE = "publication:manage";

  private final GuestAuthorRepository guests = mock(GuestAuthorRepository.class);
  private final GuestStepUpChallengeRepository challenges =
      mock(GuestStepUpChallengeRepository.class);
  private final PasswordEncoder passwords = mock(PasswordEncoder.class);
  private final GuestAuthorEntity guest = new GuestAuthorEntity("hash", T0.minusSeconds(60));

  @Test
  void locksAfterTheFifthFailureForTenMinutesOnTheInjectedClock() {
    for (int failure = 0; failure < 4; failure++) guest.recordFailure(null);
    when(guests.findByPublicId(guest.getPublicId())).thenReturn(Optional.of(guest));
    when(guests.saveAndFlush(any(GuestAuthorEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(challenges.save(any(GuestStepUpChallengeEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(passwords.matches("wrong", "hash")).thenReturn(false);
    when(passwords.matches("right", "hash")).thenReturn(true);

    assertThat(statusOf(() -> serviceAt(T0).issue(guest.getPublicId(), SCOPE, "wrong")))
        .isEqualTo(401);
    assertThat(guest.getLockedUntil()).isEqualTo(T0.plus(Duration.ofMinutes(10)));

    Instant stillLocked = T0.plus(Duration.ofMinutes(10)).minusSeconds(1);
    assertThat(statusOf(() -> serviceAt(stillLocked).issue(guest.getPublicId(), SCOPE, "right")))
        .isEqualTo(429);

    Instant unlocked = T0.plus(Duration.ofMinutes(10));
    GuestStepUpService.Challenge challenge =
        serviceAt(unlocked).issue(guest.getPublicId(), SCOPE, "right");
    assertThat(challenge.expiresAt()).isEqualTo(unlocked.plus(Duration.ofMinutes(5)));
  }

  private int statusOf(Runnable call) {
    ResponseStatusException exception =
        org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class, call::run);
    HttpStatusCode status = exception.getStatusCode();
    return status.value();
  }

  private GuestStepUpService serviceAt(Instant now) {
    return new GuestStepUpService(guests, challenges, passwords, Clock.fixed(now, ZoneOffset.UTC));
  }
}
