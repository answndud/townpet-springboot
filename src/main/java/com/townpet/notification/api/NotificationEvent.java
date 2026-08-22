package com.townpet.notification.api;

import java.util.UUID;
import org.springframework.lang.Nullable;

public record NotificationEvent(
    UUID recipientMemberId,
    UUID eventId,
    @Nullable UUID actorMemberId,
    String type,
    String title,
    String body) {}
