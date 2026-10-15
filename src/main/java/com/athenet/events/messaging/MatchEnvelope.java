package com.athenet.events.messaging;

/** Sobre de partido.actualizado. Record concreto para que Jackson sepa el tipo de data. */
public record MatchEnvelope(
        String messageId,
        String occurredAt,
        String type,
        MatchMessage data
) {
}
