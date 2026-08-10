package com.townpet.member;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberProfileRepository extends JpaRepository<MemberProfileEntity, UUID> {
  Optional<MemberProfileEntity> findByMemberId(UUID memberId);
}
