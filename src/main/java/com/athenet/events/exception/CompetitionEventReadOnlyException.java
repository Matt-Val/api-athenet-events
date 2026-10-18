package com.athenet.events.exception;

public class CompetitionEventReadOnlyException extends RuntimeException {
    public CompetitionEventReadOnlyException() {
        super("Los encuentros se gestionan desde su competencia.");
    }
}
