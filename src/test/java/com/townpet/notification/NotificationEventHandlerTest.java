package com.townpet.notification;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.townpet.notification.api.NotificationEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerTest {
  private static final UUID RECIPIENT = UUID.fromString("00000000-0000-4000-8200-000000000001");
  private static final UUID ACTOR = UUID.fromString("00000000-0000-4000-8200-000000000002");
  private static final UUID EVENT_ID = UUID.fromString("00000000-0000-4000-8200-000000000003");

  @Mock NotificationRepository notifications;

  @Test
  void insertsOnlyWhenEventIdWasNotSeen() {
    when(notifications.insertIfAbsent(
            any(), eq(RECIPIENT), eq(EVENT_ID), eq("REACTION"), eq("title"), eq("body")))
        .thenReturn(1, 0);
    NotificationEventHandler handler = new NotificationEventHandler(notifications);
    NotificationEvent event = new NotificationEvent(RECIPIENT, EVENT_ID, ACTOR, "REACTION", "title", "body");

    handler.handle(event);
    handler.handle(event);

    verify(notifications, org.mockito.Mockito.times(2))
        .insertIfAbsent(any(), eq(RECIPIENT), eq(EVENT_ID), eq("REACTION"), eq("title"), eq("body"));
  }

  @Test
  void unexpectedRepositoryFailureIsNotClassifiedAsDuplicate() {
    RuntimeException failure = new RuntimeException("database unavailable");
    when(notifications.insertIfAbsent(
            any(), eq(RECIPIENT), eq(EVENT_ID), eq("REACTION"), eq("title"), eq("body")))
        .thenThrow(failure);
    NotificationEventHandler handler = new NotificationEventHandler(notifications);

    assertThrows(
        RuntimeException.class,
        () -> handler.handle(new NotificationEvent(RECIPIENT, EVENT_ID, ACTOR, "REACTION", "title", "body")));
  }
}
