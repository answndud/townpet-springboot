package com.townpet.notification;

import com.townpet.common.CurrentMember;
import com.townpet.common.MemberOnly;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping({"/api/v1/notifications", "/api/notifications"})
class NotificationController {
  private final NotificationRepository notifications;

  NotificationController(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  @GetMapping
  @MemberOnly
  List<Response> list(
      @CurrentMember UUID currentMemberId,
      @RequestParam(defaultValue = "false") boolean unread) {
    UUID memberId = currentMemberId;
    var items =
        unread
            ? notifications.findTop100ByRecipientMemberIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(
                memberId)
            : notifications.findTop100ByRecipientMemberIdOrderByCreatedAtDescIdDesc(memberId);
    return items.stream().map(NotificationController::response).toList();
  }

  @GetMapping("/unread-count")
  @MemberOnly
  CountResponse unreadCount(@CurrentMember UUID currentMemberId) {
    return new CountResponse(
        notifications.countByRecipientMemberIdAndReadAtIsNull(currentMemberId));
  }

  @PatchMapping("/{id}/read")
  @MemberOnly
  @Transactional
  Response markRead(@CurrentMember UUID currentMemberId, @PathVariable UUID id) {
    NotificationEntity notification =
        notifications
            .findById(id)
            .filter(item -> item.getRecipientMemberId().equals(currentMemberId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    notification.markRead();
    return response(notification);
  }

  private static Response response(NotificationEntity item) {
    return new Response(
        item.getId(),
        item.getType(),
        item.getTitle(),
        item.getBody(),
        item.getReadAt(),
        item.getCreatedAt());
  }

  record Response(
      UUID id,
      String type,
      String title,
      String body,
      @Nullable Instant readAt,
      Instant createdAt) {}

  record CountResponse(long count) {}
}
