package com.athenet.events.exception;

import java.util.List;

/** Reglas propias del evento incumplidas: se responde 422 con un fieldError por regla. */
public class EventValidationException extends RuntimeException {

    private final transient List<FieldError> fieldErrors;

    public EventValidationException(List<FieldError> fieldErrors) {
        super("El evento tiene datos inválidos.");
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
