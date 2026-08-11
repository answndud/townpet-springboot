package com.townpet.notification;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
  List<NotificationEntity> findByRecipientMemberIdOrderByCreatedAtDesc(UUID memberId);
}
