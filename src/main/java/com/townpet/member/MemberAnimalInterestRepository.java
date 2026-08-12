package com.townpet.member;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberAnimalInterestRepository
    extends JpaRepository<MemberAnimalInterestEntity, UUID> {
  List<MemberAnimalInterestEntity> findAllByMemberId(UUID memberId);

  void deleteAllByMemberId(UUID memberId);
}
