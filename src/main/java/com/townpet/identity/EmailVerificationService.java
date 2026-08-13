package com.townpet.identity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailVerificationService {
  private final CredentialRepository credentials;
  private final EmailVerificationTokenRepository tokens;
  private final AuthAuditRepository audit;
  private final ApplicationEventPublisher events;
  private final AccountTokenCipher cipher;

  public EmailVerificationService(
      CredentialRepository credentials,
      EmailVerificationTokenRepository tokens,
      AuthAuditRepository audit,
      ApplicationEventPublisher events,
      AccountTokenCipher cipher) {
    this.credentials = credentials;
    this.tokens = tokens;
    this.audit = audit;
    this.events = events;
    this.cipher = cipher;
  }

  @Transactional
  public void request(String email) {
    CredentialEntity credential = credentials.findByEmailIgnoreCase(email.trim()).orElse(null);
    if (credential == null || credential.isLifecycleLocked() || credential.isEmailVerified()) {
      return;
    }

    Instant now = Instant.now();
    UUID memberId = credential.getMemberId();
    String rawToken = SecureToken.create();
    tokens.save(
        new EmailVerificationTokenEntity(
            memberId, SecureToken.hash(rawToken), now.plus(1, ChronoUnit.HOURS)));
    events.publishEvent(
        new AccountTokenDeliveryRequested(
            AccountTokenPurpose.EMAIL_VERIFICATION,
            credential.getEmail(),
            cipher.encrypt(rawToken)));
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
