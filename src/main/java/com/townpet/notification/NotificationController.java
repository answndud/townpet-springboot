package com.townpet.notification;

import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
  List<Response> list(@AuthenticationPrincipal UserDetails principal) {
    return notifications.findByRecipientMemberIdOrderByCreatedAtDesc(memberId(principal)).stream()
        .map(NotificationController::response)
        .toList();
  }

  @PatchMapping("/{id}/read")
  Response markRead(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id) {
    NotificationEntity notification =
        notifications
            .findById(id)
            .filter(item -> item.getRecipientMemberId().equals(memberId(principal)))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    notification.markRead();
    return response(notification);
  }

  private static UUID memberId(UserDetails principal) {
    try {
      return UUID.fromString(principal.getUsername());
    } catch (Exception exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
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
      UUID id, String type, String title, String body, Instant readAt, Instant createdAt) {}
}
