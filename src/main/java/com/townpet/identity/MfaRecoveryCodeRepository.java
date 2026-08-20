package com.townpet.identity;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCodeEntity, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<MfaRecoveryCodeEntity> findByMemberIdAndUsedAtIsNull(UUID memberId);

  void deleteByMemberId(UUID memberId);
}
