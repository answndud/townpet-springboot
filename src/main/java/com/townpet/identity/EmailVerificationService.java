package com.townpet.identity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailVerificationService {
  private final CredentialRepository credentials;
  private final EmailVerificationTokenRepository tokens;
  private final AuthAuditRepository audit;

  public EmailVerificationService(
      CredentialRepository credentials,
      EmailVerificationTokenRepository tokens,
      AuthAuditRepository audit) {
    this.credentials = credentials;
    this.tokens = tokens;
    this.audit = audit;
  }

  @Transactional
  public Optional<String> request(String email) {
    Optional<CredentialEntity> credential = credentials.findByEmailIgnoreCase(email.trim());
    if (credential.isEmpty()
        || credential.orElseThrow().isLifecycleLocked()
        || credential.orElseThrow().isEmailVerified()) {
      return Optional.empty();
    }

    Instant now = Instant.now();
    UUID memberId = credential.orElseThrow().getMemberId();
    String rawToken = SecureToken.create();
    tokens.save(
        new EmailVerificationTokenEntity(
            memberId, SecureToken.hash(rawToken), now.plus(1, ChronoUnit.HOURS)));
    return Optional.of(rawToken);
  }

  @Transactional
  public void confirm(String rawToken) {
    Instant now = Instant.now();
    EmailVerificationTokenEntity token =
        tokens
            .findByTokenHashAndExpiresAtAfter(SecureToken.hash(rawToken), now)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid or expired email verification token"));
    CredentialEntity credential =
        credentials
            .findByMemberId(token.getMemberId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credential not found"));
    if (credential.isLifecycleLocked()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Credential lifecycle is locked");
    }

    if (!credential.isEmailVerified()) {
      credential.verifyEmail(now);
      audit.save(new AuthAuditEntity(credential.getMemberId(), "EMAIL_VERIFIED"));
    }
    tokens.deleteAllByMemberId(credential.getMemberId());
  }
}
