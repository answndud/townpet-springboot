package com.townpet.identity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository
    extends JpaRepository<EmailVerificationTokenEntity, UUID> {
  Optional<EmailVerificationTokenEntity> findByTokenHashAndExpiresAtAfter(
      String tokenHash, Instant now);

  long countByMemberIdAndCreatedAtAfter(UUID memberId, Instant createdAt);

  void deleteAllByMemberId(UUID memberId);
}
