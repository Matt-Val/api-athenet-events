package com.athenet.events.messaging;

/**
 * Data de evento.publicado: lo que ms-notificaciones necesita para avisar del evento.
 * Fechas y horas van como texto (yyyy-MM-dd / HH:mm).
 */
public record EventPublishedMessage(
        String internalId,
        String title,
        String description,
        String type,
        boolean official,
        String eventDate,
        String startTime,
        String endDate,
        String endTime,
        String location,
        String address,
        String coverImage,
        Long sportId,
        Long organizerInstitutionId,
        Long organizerCampusId,
        Long organizerTeamId,
        Long teamOneId,
        Long teamTwoId,
        String delegateOid,
        String delegateDisplayName
) {
}
