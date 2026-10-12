package com.athenet.events.service;

import com.athenet.events.dto.EventRequest;
import com.athenet.events.exception.FieldError;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Reglas propias del evento, que no necesitan consultar otro servicio. Corre
 * después de Bean Validation obligatorios y formatos del DTO).
 *
 * La integridad referencial contra ms-institution (sede ∈ institución,
 * equipo ∈ sede, deporte y división del equipo) es responsabilidad del BFF:
 * este servicio no conoce esa base.
 */
@Component
public class EventValidator {

    /**
     * @param request body recibido
     * @param routeInternalId internalId de la ruta en un PUT; null en un POST
     * @return un FieldError por regla incumplida (vacío si el evento es válido)
     */
    public List<FieldError> validate(EventRequest request, String routeInternalId) {
        List<FieldError> errors = new ArrayList<>();

        if (routeInternalId != null && !routeInternalId.equals(request.internalId())) {
            errors.add(new FieldError("internalId", "IMMUTABLE", "El ID interno no se puede modificar."));
        }

        validateSchedule(request, errors);
        validateDelegatePair(request, errors);

        if (request.type() != null) {
            if (request.type().requiresMatchup()) {
                validateMatch(request, errors);
            } else {
                validateNonMatch(request, errors);
            }
        }
        return errors;
    }

    /** El término es posterior al inicio. Puede ser al día siguiente o varios días después. */
    private void validateSchedule(EventRequest r, List<FieldError> errors) {
        if (r.eventDate() == null || r.startTime() == null || r.endDate() == null || r.endTime() == null) {
            return; // los obligatorios los reporta Bean Validation
        }
        if (r.endDate().isBefore(r.eventDate())) {
            errors.add(endBeforeStart("endDate"));
        } else if (r.endDate().equals(r.eventDate()) && !r.endTime().isAfter(r.startTime())) {
            errors.add(endBeforeStart("endTime"));
        }
    }

    /** El nombre del delegado es un snapshot de su oid: van juntos o NO van */
    private void validateDelegatePair(EventRequest r, List<FieldError> errors) {
        boolean hasOid = !isBlank(r.delegateOid());
        boolean hasName = !isBlank(r.delegateDisplayName());
        if (hasOid && !hasName) {
            errors.add(required("delegateDisplayName"));
        } else if (hasName && !hasOid) {
            errors.add(required("delegateOid"));
        }
    }

    /** Partido: deporte, dos equipos distintos y delegado. El organizador será la competencia. */
    private void validateMatch(EventRequest r, List<FieldError> errors) {
        if (r.sportId() == null) errors.add(required("sportId"));
        if (r.teamOneId() == null) errors.add(required("teamOneId"));
        if (r.teamTwoId() == null) errors.add(required("teamTwoId"));
        if (isBlank(r.delegateOid()) && isBlank(r.delegateDisplayName())) errors.add(required("delegateOid"));

        if (r.teamOneId() != null && r.teamOneId().equals(r.teamTwoId())) {
            errors.add(new FieldError("teamTwoId", "SAME_TEAM", "Un equipo no puede enfrentarse a sí mismo."));
        }

        notAllowed(r.organizerInstitutionId(), "organizerInstitutionId", "ORGANIZER_NOT_ALLOWED", errors);
        notAllowed(r.organizerCampusId(), "organizerCampusId", "ORGANIZER_NOT_ALLOWED", errors);
        notAllowed(r.organizerTeamId(), "organizerTeamId", "ORGANIZER_NOT_ALLOWED", errors);
    }

    /** Otro tipo: lo organiza una institución y una sede; equipo, deporte y delegado opcionales. */
    private void validateNonMatch(EventRequest r, List<FieldError> errors) {
        if (r.organizerInstitutionId() == null) errors.add(required("organizerInstitutionId"));
        if (r.organizerCampusId() == null) errors.add(required("organizerCampusId"));

        notAllowed(r.teamOneId(), "teamOneId", "MATCHUP_NOT_ALLOWED", errors);
        notAllowed(r.teamTwoId(), "teamTwoId", "MATCHUP_NOT_ALLOWED", errors);
    }

    

    private static void notAllowed(Long value, String field, String code, List<FieldError> errors) {
        if (value != null) {
            errors.add(new FieldError(field, code, "Este campo no aplica a este tipo de evento."));
        }
    }

    private static FieldError required(String field) {
        return new FieldError(field, "REQUIRED", "Este campo es obligatorio.");
    }

    private static FieldError endBeforeStart(String field) {
        return new FieldError(field, "END_BEFORE_START", "El término debe ser posterior al inicio.");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
