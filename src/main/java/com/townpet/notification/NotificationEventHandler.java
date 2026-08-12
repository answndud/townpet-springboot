package com.townpet.notification;

import com.townpet.common.UuidV7;
import com.townpet.notification.api.NotificationEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class NotificationEventHandler {
  private final NotificationRepository notifications;

  NotificationEventHandler(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  @ApplicationModuleListener
  void handle(NotificationEvent event) {
    if (event.recipientMemberId().equals(event.actorMemberId())) return;
    notifications.save(
        new NotificationEntity(
            UuidV7.randomUuid(),
            event.recipientMemberId(),
            event.type(),
            event.title(),
            event.body()));
  }
}
