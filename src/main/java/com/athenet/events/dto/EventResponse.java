package com.athenet.events.dto;

import com.athenet.events.model.EventStatus;
import com.athenet.events.model.EventType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Evento tal como lo guarda este servicio: plano, con IDs. El BFF resuelve las
 * referencias (deporte, institución, sede, equipos) y arma la lectura anidada.
 */
public record EventResponse(
        Long id,
        String internalId,
        String title,
        String description,
        String descriptionOpt,
        EventType type,
        EventStatus status,
        @JsonProperty("isOfficial") boolean isOfficial,

        LocalDate eventDate,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        LocalDate endDate,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,

        String location,
        String address,
        String coverImage,
        List<String> photos,

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
