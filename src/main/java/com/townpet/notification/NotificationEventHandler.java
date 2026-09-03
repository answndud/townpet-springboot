package com.townpet.notification;

import com.townpet.common.UuidV7;
import com.townpet.notification.api.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
    try {
      notifications.save(
          new NotificationEntity(
              UuidV7.randomUuid(),
              event.eventId(),
              event.recipientMemberId(),
              event.type(),
              event.title(),
              event.body()));
      log.info(
          "event=notification_consumer outcome=success event_id={} type={}",
          event.eventId(),
          event.type());
    } catch (DataIntegrityViolationException exception) {
      // A replayed publication already produced this notification; keep the consumer idempotent.
      log.info(
          "event=notification_consumer outcome=duplicate event_id={} type={} reason=data_integrity_violation",
          event.eventId(),
          event.type());
    }
  }
}
