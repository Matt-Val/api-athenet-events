package com.athenet.events.messaging;

/** Sobre de evento.publicado. Record concreto, igual que los sobres de ms-competition. */
public record EventPublishedEnvelope(
        String messageId,
        String occurredAt,
        String type,
        EventPublishedMessage data
) {
}
