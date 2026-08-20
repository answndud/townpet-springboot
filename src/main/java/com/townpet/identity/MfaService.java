package com.townpet.identity;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class MfaService {
  static final String SESSION_VERIFIED_ATTRIBUTE = "TOWNPET_MFA_VERIFIED";
  private static final Duration ENROLLMENT_LIFETIME = Duration.ofMinutes(10);
  private static final int RECOVERY_CODE_COUNT = 8;
  private final MfaFactorRepository factors;
  private final MfaRecoveryCodeRepository recoveryCodes;
  private final PasswordEncoder passwordEncoder;
  private final AccountTokenCipher cipher;
  private final TotpService totp;
  private final AuthAuditRepository audit;

  MfaService(
      MfaFactorRepository factors,
      MfaRecoveryCodeRepository recoveryCodes,
      PasswordEncoder passwordEncoder,
      AccountTokenCipher cipher,
      TotpService totp,
      AuthAuditRepository audit) {
    this.factors = factors;
    this.recoveryCodes = recoveryCodes;
    this.passwordEncoder = passwordEncoder;
    this.cipher = cipher;
    this.totp = totp;
    this.audit = audit;
  }

  @Transactional
  Enrollment startEnrollment(UUID memberId) {
    MfaFactorEntity factor = factors.findLockedByMemberId(memberId).orElse(null);
    if (factor != null && factor.getEnabledAt() != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "MFA is already enabled");
    }
    String secret = totp.newSecret();
    Instant now = Instant.now();
    if (factor == null) {
      factors.save(
          new MfaFactorEntity(memberId, cipher.encrypt(secret), now.plus(ENROLLMENT_LIFETIME)));
    } else {
      factor.replaceEnrollment(cipher.encrypt(secret), now.plus(ENROLLMENT_LIFETIME), now);
      factors.save(factor);
    }
    audit.save(new AuthAuditEntity(memberId, "MFA_ENROLLMENT_STARTED"));
    return new Enrollment(secret, totp.otpauthUri(secret, memberId), now.plus(ENROLLMENT_LIFETIME));
  }

  @Transactional
  List<String> confirmEnrollment(UUID memberId, String code) {
    MfaFactorEntity factor =
        factors
            .findLockedByMemberId(memberId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "MFA enrollment is required"));
    Instant now = Instant.now();
    if (factor.getEnabledAt() != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "MFA is already enabled");
    }
    if (factor.getEnrollmentExpiresAt().isBefore(now)
        || !totp.matches(cipher.decrypt(factor.getSecretCiphertext()), code, now)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired MFA code");
    }
    factor.enable(now);
    recoveryCodes.deleteByMemberId(memberId);
    List<String> rawCodes = new ArrayList<>();
    for (int index = 0; index < RECOVERY_CODE_COUNT; index++) {
      String rawCode = recoveryCode();
      rawCodes.add(rawCode);
      recoveryCodes.save(new MfaRecoveryCodeEntity(memberId, passwordEncoder.encode(rawCode)));
    }
    audit.save(new AuthAuditEntity(memberId, "MFA_ENROLLED"));
    return rawCodes;
  }

  @Transactional(readOnly = true)
  boolean isEnabled(UUID memberId) {
    return factors.findById(memberId).map(factor -> factor.getEnabledAt() != null).orElse(false);
  }

  @Transactional
  void verify(UUID memberId, String code) {
    MfaFactorEntity factor =
        factors
            .findById(memberId)
            .filter(item -> item.getEnabledAt() != null)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "MFA enrollment is required"));
    if (!totp.matches(cipher.decrypt(factor.getSecretCiphertext()), code, Instant.now())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
    }
    audit.save(new AuthAuditEntity(memberId, "MFA_VERIFIED"));
  }

  @Transactional
  void useRecoveryCode(UUID memberId, String rawCode) {
    if (factors.findById(memberId).filter(item -> item.getEnabledAt() != null).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MFA enrollment is required");
    }
    List<MfaRecoveryCodeEntity> codes = recoveryCodes.findByMemberIdAndUsedAtIsNull(memberId);
    for (MfaRecoveryCodeEntity code : codes) {
      if (passwordEncoder.matches(rawCode, code.getCodeHash())) {
        code.markUsed(Instant.now());
        recoveryCodes.save(code);
        audit.save(new AuthAuditEntity(memberId, "MFA_RECOVERY_USED"));
        return;
      }
    }
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA recovery code");
  }

  private static String recoveryCode() {
    return UUID.randomUUID()
        .toString()
        .replace("-", "")
        .substring(0, 16)
        .toUpperCase(java.util.Locale.ROOT);
  }

  record Enrollment(String secret, String otpauthUri, Instant expiresAt) {}
}
