package com.athenet.events.exception;

import java.time.LocalDateTime;
import java.util.List;

// Mismo formato que institution-service (timestamp, status, error, message), más
// `code` y `fieldErrors` para que el BFF y el front puedan pintar cada error en
// su campo. Los campos nuevos solo se agregan: el formato sigue siendo compatible.
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        List<FieldError> fieldErrors
) {
}
