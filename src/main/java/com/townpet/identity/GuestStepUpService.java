package com.townpet.identity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class GuestStepUpService {
  private static final Pattern SCOPE = Pattern.compile("[A-Za-z0-9:_-]{1,80}");
  private final GuestAuthorRepository guests;
  private final GuestStepUpChallengeRepository challenges;
  private final PasswordEncoder passwords;

  GuestStepUpService(
      GuestAuthorRepository guests,
      GuestStepUpChallengeRepository challenges,
      PasswordEncoder passwords) {
    this.guests = guests;
    this.challenges = challenges;
    this.passwords = passwords;
  }

  @Transactional
  GuestAuthorEntity createGuest(String password) {
    validatePassword(password);
    return guests.save(new GuestAuthorEntity(passwords.encode(password)));
  }

  @Transactional
  Challenge issue(UUID publicId, String scope, String password) {
    if (!SCOPE.matcher(scope).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid step-up scope");
    }
    GuestAuthorEntity guest =
        guests
            .findByPublicId(publicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    Instant now = Instant.now();
    if (guest.getLockedUntil() != null && guest.getLockedUntil().isAfter(now)) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Guest credential is temporarily locked");
    }
    if (!passwords.matches(password, guest.getManagementPasswordHash())) {
      Instant lock = guest.getFailedAttempts() + 1 >= 5 ? now.plus(Duration.ofMinutes(10)) : null;
      guest.recordFailure(lock);
      guests.saveAndFlush(guest);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid guest credential");
    }
    guest.clearFailures();
    String raw = SecureToken.create();
    Instant expiresAt = now.plus(Duration.ofMinutes(5));
    challenges.save(
        new GuestStepUpChallengeEntity(guest.getId(), scope, SecureToken.hash(raw), expiresAt));
    return new Challenge(scope, expiresAt, raw);
  }

  @Transactional
  String consume(String rawToken, String expectedScope) {
    GuestStepUpChallengeEntity challenge =
        challenges
            .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                SecureToken.hash(rawToken), Instant.now())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid or expired step-up"));
    if (!challenge.getScope().equals(expectedScope)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Step-up scope mismatch");
    }
    challenge.markUsed(Instant.now());
    challenges.saveAndFlush(challenge);
    return challenge.getScope();
  }

  private static void validatePassword(String password) {
    if (password == null || password.length() < 8 || password.length() > 72) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Guest password must be 8 to 72 characters");
    }
  }

  record Challenge(String scope, Instant expiresAt, String rawToken) {}
}
