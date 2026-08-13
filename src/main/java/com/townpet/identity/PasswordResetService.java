package com.townpet.identity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordResetService {
  private static final Pattern STRONG_PASSWORD =
      Pattern.compile(
          "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])(?!.*(.)\\1{3,}).{10,72}$");
  private static final Set<String> COMMON_PASSWORDS =
      Set.of(
          "12345678",
          "123456789",
          "1234567890",
          "password",
          "password1",
          "password123",
          "qwerty123",
          "asdf1234",
          "letmein",
          "11111111",
          "00000000",
          "admin1234");

  private final CredentialRepository credentials;
  private final PasswordResetTokenRepository tokens;
  private final AuthAuditRepository audit;
  private final PasswordEncoder passwordEncoder;
  private final SessionRevocationService sessionRevocation;
  private final ApplicationEventPublisher events;
  private final AccountTokenCipher cipher;

  public PasswordResetService(
      CredentialRepository credentials,
      PasswordResetTokenRepository tokens,
      AuthAuditRepository audit,
      PasswordEncoder passwordEncoder,
      SessionRevocationService sessionRevocation,
      ApplicationEventPublisher events,
      AccountTokenCipher cipher) {
    this.credentials = credentials;
    this.tokens = tokens;
    this.audit = audit;
    this.passwordEncoder = passwordEncoder;
    this.sessionRevocation = sessionRevocation;
    this.events = events;
    this.cipher = cipher;
  }

  @Transactional
  public void request(String email) {
    CredentialEntity credential = credentials.findByEmailIgnoreCase(email.trim()).orElse(null);
    if (credential == null || credential.isLifecycleLocked()) {
      return;
    }

    Instant now = Instant.now();
    UUID memberId = credential.getMemberId();
    tokens.deleteConsumedOrExpired(memberId, now);
    String rawToken = SecureToken.create();
    tokens.save(
        new PasswordResetTokenEntity(
            memberId, SecureToken.hash(rawToken), now.plus(1, ChronoUnit.HOURS)));
    events.publishEvent(
        new AccountTokenDeliveryRequested(
            AccountTokenPurpose.PASSWORD_RESET, credential.getEmail(), cipher.encrypt(rawToken)));
  }

  @Transactional
  public void confirm(String rawToken, String newPassword) {
    validatePassword(newPassword);
    Instant now = Instant.now();
    PasswordResetTokenEntity token =
        tokens
            .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(SecureToken.hash(rawToken), now)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid or expired password reset token"));
    CredentialEntity credential =
        credentials
            .findByMemberId(token.getMemberId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credential"));
    if (credential.isLifecycleLocked()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Credential lifecycle is locked");
    }

    credential.changePassword(passwordEncoder.encode(newPassword));
    token.markUsed(now);
    audit.save(new AuthAuditEntity(credential.getMemberId(), "PASSWORD_RESET"));
    tokens.flush();
    tokens.deleteConsumedOrExpired(credential.getMemberId(), now);
    sessionRevocation.revokeAll(credential.getMemberId());
  }

  private static void validatePassword(String password) {
    if (!STRONG_PASSWORD.matcher(password).matches()
        || COMMON_PASSWORDS.contains(password.trim().toLowerCase(Locale.ROOT))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password does not meet policy");
    }
  }
}
