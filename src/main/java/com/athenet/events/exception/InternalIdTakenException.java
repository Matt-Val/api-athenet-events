package com.athenet.events.exception;

/** El internalId ya pertenece a otro evento: 409. */
public class InternalIdTakenException extends RuntimeException {

    public InternalIdTakenException(String internalId) {
        super("El internalId " + internalId + " ya está en uso.");
    }
}
