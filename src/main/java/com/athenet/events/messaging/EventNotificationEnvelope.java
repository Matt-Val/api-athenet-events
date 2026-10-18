package com.athenet.events.messaging;

public record EventNotificationEnvelope(String messageId, String occurredAt, String type,
        Long version, EventNotificationData data) {}
