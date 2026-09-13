package com.athenet.events.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(String internalId) {
        super("Error: Evento no encontrado con ID: " + internalId);
    }

    public EventNotFoundException(Long id) {
        super("Error: Evento no encontrado con ID: " + id);
    }
}
