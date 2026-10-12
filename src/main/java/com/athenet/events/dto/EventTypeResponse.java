package com.athenet.events.dto;

import com.athenet.events.model.EventType;

/** Entrada del catálogo de tipos: `requiresMatchup` decide la forma del formulario. */
public record EventTypeResponse(String code, String label, boolean requiresMatchup) {

    public static EventTypeResponse from(EventType type) {
        return new EventTypeResponse(type.name(), type.getLabel(), type.requiresMatchup());
    }
}
