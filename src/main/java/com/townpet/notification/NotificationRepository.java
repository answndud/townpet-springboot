package com.townpet.notification;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
  List<NotificationEntity> findTop100ByRecipientMemberIdOrderByCreatedAtDescIdDesc(UUID memberId);

  List<NotificationEntity> findTop100ByRecipientMemberIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(
      UUID memberId);

  long countByRecipientMemberIdAndReadAtIsNull(UUID memberId);
}
