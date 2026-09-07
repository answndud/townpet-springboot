package com.townpet.notification;

import java.util.*;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
  @Modifying
  @Query(
      value = """
          INSERT INTO notification
              (id, recipient_member_id, event_id, type, title, body, created_at)
          VALUES
              (:id, :recipientMemberId, :eventId, :type, :title, :body, CURRENT_TIMESTAMP)
          ON CONFLICT (event_id) DO NOTHING
          """,
      nativeQuery = true)
  int insertIfAbsent(
      @Param("id") UUID id,
      @Param("recipientMemberId") UUID recipientMemberId,
      @Param("eventId") UUID eventId,
      @Param("type") String type,
      @Param("title") String title,
      @Param("body") String body);

  List<NotificationEntity> findTop100ByRecipientMemberIdOrderByCreatedAtDescIdDesc(UUID memberId);

  List<NotificationEntity> findTop100ByRecipientMemberIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(
      UUID memberId);

  long countByRecipientMemberIdAndReadAtIsNull(UUID memberId);
}
