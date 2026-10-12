package com.athenet.events.exception;

/**
 * Error de un campo concreto. `field` es el nombre exacto de la clave del body,
 * que es también el nombre del control del formulario en el front.
 */
public record FieldError(String field, String code, String message) {
}
