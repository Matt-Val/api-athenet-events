package com.athenet.events.model;

/**
 * Tipo de evento. `requiresMatchup` define la forma del evento: un partido
 * lleva dos equipos; el resto lleva organizador. Se expone en GET /api/admin/event-types para no duplicar regla
 */
public enum EventType {
    MATCH("Partido", true),
    MEETING("Reunión", false),
    TRACKDAY("Jornada de Pista", false),
    OTHER("Otro", false);

    private final String label;
    private final boolean requiresMatchup;

    EventType(String label, boolean requiresMatchup) {
        this.label = label;
        this.requiresMatchup = requiresMatchup;
    }

    public String getLabel() {
        return label;
    }

    public boolean requiresMatchup() {
        return requiresMatchup;
    }
}
