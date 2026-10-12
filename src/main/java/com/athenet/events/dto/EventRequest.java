package com.athenet.events.dto;

import com.athenet.events.model.EventStatus;
import com.athenet.events.model.EventType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Body de POST/PUT: plano, solo IDs (contrato del BFF, §2). Los IDs de
 * institución y sede de cada equipo no llegan aquí: el BFF los usa para validar
 * la cadena y reenvía solo teamOneId/teamTwoId.
 *
 * Un campo desconocido o un objeto anidado se rechaza con 400.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record EventRequest(
        @NotBlank @Size(max = 255) String internalId,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String description,
        String descriptionOpt,
        @NotNull EventType type,
        @NotNull EventStatus status,
        @JsonProperty("isOfficial") boolean isOfficial,

        @NotNull LocalDate eventDate,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @NotNull LocalDate endDate,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime,

        @Size(max = 255) String location,
        @Size(max = 255) String address,
        @NotBlank @Size(max = 255) String coverImage,
        List<@NotBlank @Size(max = 255) String> photos,

        Long sportId,
        Long organizerInstitutionId,
        Long organizerCampusId,
        Long organizerTeamId,
        Long teamOneId,
        Long teamTwoId,

        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        String delegateOid,
        @Size(max = 256) String delegateDisplayName
) {
}
