package com.townpet.notification;

import com.townpet.common.UuidV7;
import com.townpet.notification.api.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class NotificationEventHandler {
  private static final Logger log = LoggerFactory.getLogger(NotificationEventHandler.class);
  private final NotificationRepository notifications;

  NotificationEventHandler(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  @ApplicationModuleListener
  void handle(NotificationEvent event) {
    if (event.recipientMemberId().equals(event.actorMemberId())) {
      log.debug(
          "event=notification_consumer outcome=skipped_self recipient_id={}",
          event.recipientMemberId());
      return;
    }
    int inserted =
        notifications.insertIfAbsent(
            UuidV7.randomUuid(),
            event.recipientMemberId(),
            event.eventId(),
            event.type(),
            event.title(),
            event.body());
    if (inserted == 1) {
      log.info(
          "event=notification_consumer outcome=success event_id={} type={}",
          event.eventId(),
          event.type());
    } else {
      // A replayed publication already produced this notification; keep the consumer idempotent.
      log.info(
          "event=notification_consumer outcome=duplicate event_id={} type={} reason=event_id_conflict",
          event.eventId(),
          event.type());
    }
  }
}
