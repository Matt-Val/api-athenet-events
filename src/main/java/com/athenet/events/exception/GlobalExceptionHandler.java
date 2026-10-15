package com.athenet.events.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Traduce las excepciones al formato ApiError. Los errores atribuibles a un
 * campo van en `fieldErrors`, con el nombre exacto de la clave del body.
 *
 * - 400 MALFORMED_REQUEST: el body no se puede leer (JSON inválido, fecha u
 *   hora con otro formato, campo desconocido u objeto anidado).
 * - 422 VALIDATION_FAILED: el body se leyó pero incumple alguna regla.
 * - 404 EVENT_NOT_FOUND / 409 INTERNAL_ID_TAKEN / 409 MANAGED_BY_COMPETITION.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Anotaciones de Bean Validation → códigos del contrato. */
    private static final Map<String, String> CONSTRAINT_CODES = Map.of(
            "NotNull", "REQUIRED",
            "NotBlank", "REQUIRED",
            "Size", "TOO_LONG",
            "Pattern", "INVALID_FORMAT"
    );

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(EventNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", ex.getMessage(), List.of());
    }

    @ExceptionHandler(InternalIdTakenException.class)
    public ResponseEntity<ApiError> handleInternalIdTaken(InternalIdTakenException ex) {
        return build(HttpStatus.CONFLICT, "INTERNAL_ID_TAKEN", ex.getMessage(),
                List.of(new FieldError("internalId", "INTERNAL_ID_TAKEN", "Ese ID interno ya está en uso.")));
    }

    @ExceptionHandler(ManagedByCompetitionException.class)
    public ResponseEntity<ApiError> handleManagedByCompetition(ManagedByCompetitionException ex) {
        return build(HttpStatus.CONFLICT, "MANAGED_BY_COMPETITION", ex.getMessage(), List.of());
    }

    // Red de seguridad: si dos creaciones con el mismo internal_id se cruzan entre
    // el existsByInternalId y el save, la constraint unique de la tabla responde.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, "CONFLICT",
                "El evento no se pudo guardar: entra en conflicto con datos existentes.", List.of());
    }

    @ExceptionHandler(EventValidationException.class)
    public ResponseEntity<ApiError> handleEventValidation(EventValidationException ex) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED", ex.getMessage(), ex.getFieldErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new FieldError(
                        err.getField(),
                        CONSTRAINT_CODES.getOrDefault(err.getCode(), "INVALID"),
                        err.getDefaultMessage()))
                .toList();
        return build(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED", "El evento tiene datos inválidos.", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex) {
        // La excepción de Jackson (la que trae la ruta del campo) puede estar envuelta
        // o envolver a su vez la causa raíz (p. ej. DateTimeParseException).
        JacksonException jackson = null;
        for (Throwable t = ex; t != null && jackson == null; t = t.getCause()) {
            if (t instanceof JacksonException je) jackson = je;
        }
        List<FieldError> fieldErrors = List.of();

        if (jackson != null && !jackson.getPath().isEmpty()) {
            String field = pathOf(jackson);
            String code = jackson instanceof UnrecognizedPropertyException ? "UNKNOWN_FIELD" : "INVALID_FORMAT";
            String message = jackson instanceof UnrecognizedPropertyException
                    ? "Campo no admitido: el body de escritura es plano, solo con IDs."
                    : "Formato inválido (fechas yyyy-MM-dd, horas HH:mm).";
            if (field != null) {
                fieldErrors = List.of(new FieldError(field, code, message));
            }
        }
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "El cuerpo de la petición no es válido.", fieldErrors);
    }

    /** Ruta del campo culpable en notación del body: `startTime`, `photos[2]`. */
    private static String pathOf(JacksonException ex) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference ref : ex.getPath()) {
            if (ref.getPropertyName() != null) {
                if (!path.isEmpty()) path.append('.');
                path.append(ref.getPropertyName());
            } else if (ref.getIndex() >= 0) {
                path.append('[').append(ref.getIndex()).append(']');
            }
        }
        return path.isEmpty() ? null : path.toString();
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message, List<FieldError> fieldErrors) {
        ApiError body = new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), code, message, fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
