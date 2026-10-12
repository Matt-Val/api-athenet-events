package com.athenet.events.support;

import com.athenet.events.dto.EventRequest;
import com.athenet.events.model.EventStatus;
import com.athenet.events.model.EventType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** HELPERS: Eventos válidos con la forma del contrato del BFF, para partir de ellos en los tests. */
public final class EventFixtures {

    public static final String DELEGATE_OID = "3f2b9c1e-5a7d-4c1b-9e2f-1d0a8b6c4e21";

    private EventFixtures() {
    }

    /** Partido que cruza la medianoche. */
    public static EventRequest match() {
        return new EventRequest(
                "EVT-42", "Clásico universitario", "Final del torneo.", null,
                EventType.MATCH, EventStatus.DRAFT, true,
                LocalDate.of(2026, 10, 24), LocalTime.of(22, 0),
                LocalDate.of(2026, 10, 25), LocalTime.of(0, 30),
                "Estadio USACH", null, "https://ejemplo.com/cover.jpg", List.of(),
                3L, null, null, null, 100L, 200L,
                DELEGATE_OID, "María Pérez");
    }

    /** Jornada de varios días, sin deporte, organizada por una institución. */
    public static EventRequest trackday() {
        return new EventRequest(
                "EVT-43", "Jornada de pista", "Tres días de pista.", null,
                EventType.TRACKDAY, EventStatus.DRAFT, false,
                LocalDate.of(2026, 11, 6), LocalTime.of(9, 0),
                LocalDate.of(2026, 11, 8), LocalTime.of(18, 0),
                null, null, "https://ejemplo.com/cover.jpg", List.of(),
                null, 1L, 10L, null, null, null,
                null, null);
    }

    public static final String MATCH_JSON = """
            {
              "internalId": "EVT-42",
              "title": "Clásico universitario",
              "description": "Final del torneo.",
              "descriptionOpt": null,
              "type": "MATCH",
              "status": "DRAFT",
              "isOfficial": true,
              "eventDate": "2026-10-24",
              "startTime": "22:00",
              "endDate": "2026-10-25",
              "endTime": "00:30",
              "location": "Estadio USACH",
              "address": null,
              "coverImage": "https://ejemplo.com/cover.jpg",
              "photos": [],
              "sportId": 3,
              "organizerInstitutionId": null,
              "organizerCampusId": null,
              "organizerTeamId": null,
              "teamOneId": 100,
              "teamTwoId": 200,
              "delegateOid": "3f2b9c1e-5a7d-4c1b-9e2f-1d0a8b6c4e21",
              "delegateDisplayName": "María Pérez"
            }
            """;
}
