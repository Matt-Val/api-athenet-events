package com.athenet.events.messaging;

/**
 * Data de partido.actualizado, tal como la publica ms-competition. Fecha y hora llegan
 * como texto (yyyy-MM-dd / HH:mm).
 */
public record MatchMessage(
        Long matchId,
        Long competitionId,
        Long sportId,
        String title,
        String description,
        String competitionImg,
        String date,
        String time,
        String location,
        String address,
        Long homeTeamId,
        Long awayTeamId,
        String delegateOid,
        String delegateDisplayName
) {
}
