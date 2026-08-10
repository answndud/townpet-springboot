package com.townpet.member;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberPetRepository extends JpaRepository<MemberPetEntity, UUID> {
  List<MemberPetEntity> findAllByMemberIdOrderByCreatedAtAsc(UUID memberId);

  void deleteAllByMemberId(UUID memberId);
}
