package com.townpet.identity;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface MfaFactorRepository extends JpaRepository<MfaFactorEntity, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<MfaFactorEntity> findLockedByMemberId(UUID memberId);
}
