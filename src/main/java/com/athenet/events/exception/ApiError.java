package com.athenet.events.exception;

import java.time.LocalDateTime;

// Mismo formato que institution-service, para que el front lo lea igual
// sin importar a qué microservicio le esté pegando.
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message
) {
}
