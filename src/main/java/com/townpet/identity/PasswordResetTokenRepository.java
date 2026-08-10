package com.townpet.identity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository
    extends JpaRepository<PasswordResetTokenEntity, UUID> {
  Optional<PasswordResetTokenEntity> findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
      String tokenHash, Instant now);

  @Modifying
  @Query(
      "delete from PasswordResetTokenEntity token where token.memberId = :memberId "
          + "and (token.usedAt is not null or token.expiresAt < :now)")
  void deleteConsumedOrExpired(@Param("memberId") UUID memberId, @Param("now") Instant now);
}
