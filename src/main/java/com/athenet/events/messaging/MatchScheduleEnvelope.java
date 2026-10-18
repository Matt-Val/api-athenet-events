package com.athenet.events.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchScheduleEnvelope(@NotBlank String messageId, @NotBlank String occurredAt,
        @NotNull @Pattern(regexp = "partido\\.actualizado") String type, @NotNull @Valid Data data) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(@NotNull @Positive Long matchId, @NotNull @Positive Long competitionId,
            @NotNull @Positive Long sportId, @Min(1) int scheduleVersion,
            @NotBlank @Size(max = 255) String title, @NotBlank @Size(max = 255) String description,
            @NotBlank @Size(max = 255) String coverImage,
            @NotBlank String eventDate, @NotBlank String startTime, @NotBlank String endDate, @NotBlank String endTime,
            @Size(max = 255) String location, @Size(max = 255) String address,
            @NotNull @Positive Long homeTeamId, @NotNull @Positive Long awayTeamId,
            @NotBlank @Pattern(regexp = "[0-9a-fA-F-]{36}") String delegateOid,
            @NotBlank @Size(max = 256) String delegateDisplayName,
            @NotNull @Pattern(regexp = "PUBLISHED|CANCELLED") String status) {}
}
