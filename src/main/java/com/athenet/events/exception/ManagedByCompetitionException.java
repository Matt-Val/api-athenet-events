package com.athenet.events.exception;

/** El evento es copia de un partido de ms-competition: se edita en la competencia. 409. */
public class ManagedByCompetitionException extends RuntimeException {

    public ManagedByCompetitionException(String internalId) {
        super("El evento " + internalId + " es un partido de una competencia: se gestiona desde la competencia.");
    }
}
