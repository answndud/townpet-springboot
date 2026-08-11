package com.townpet.identity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface GuestStepUpChallengeRepository extends JpaRepository<GuestStepUpChallengeEntity, UUID> {
  Optional<GuestStepUpChallengeEntity> findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
      String tokenHash, Instant now);
}
