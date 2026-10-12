package com.athenet.events.service;

import com.athenet.events.dto.EventRequest;
import com.athenet.events.exception.FieldError;
import com.athenet.events.model.EventType;
import com.athenet.events.support.EventFixtures;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventValidatorTest {

    private final EventValidator validator = new EventValidator();

    private static List<String> codesOf(List<FieldError> errors, String field) {
        return errors.stream().filter(e -> e.field().equals(field)).map(FieldError::code).toList();
    }

    /** Copia del request cambiando solo los campos indicados. */
    private static EventRequest with(EventRequest r, Changes c) {
        return new EventRequest(
                c.internalId != null ? c.internalId : r.internalId(), r.title(), r.description(), r.descriptionOpt(),
                c.type != null ? c.type : r.type(), r.status(), r.isOfficial(),
                c.eventDate != null ? c.eventDate : r.eventDate(),
                c.startTime != null ? c.startTime : r.startTime(),
                c.endDate != null ? c.endDate : r.endDate(),
                c.endTime != null ? c.endTime : r.endTime(),
                r.location(), r.address(), r.coverImage(), r.photos(),
                c.clearSport ? null : r.sportId(),
                c.organizerInstitutionId != null ? c.organizerInstitutionId : r.organizerInstitutionId(),
                r.organizerCampusId(), r.organizerTeamId(),
                c.teamOneId != null ? c.teamOneId : r.teamOneId(),
                c.teamTwoId != null ? c.teamTwoId : r.teamTwoId(),
                c.clearDelegate ? null : (c.delegateOid != null ? c.delegateOid : r.delegateOid()),
                c.clearDelegate || c.clearDelegateName ? null : r.delegateDisplayName());
    }

    private static final class Changes {
        String internalId;
        EventType type;
        LocalDate eventDate;
        LocalTime startTime;
        LocalDate endDate;
        LocalTime endTime;
        Long organizerInstitutionId;
        Long teamOneId;
        Long teamTwoId;
        String delegateOid;
        boolean clearSport;
        boolean clearDelegate;
        boolean clearDelegateName;
    }

    @Test
    void partidoQueCruzaLaMedianocheEsValido() {
        assertThat(validator.validate(EventFixtures.match(), null)).isEmpty();
    }

    @Test
    void jornadaDeVariosDiasSinDeporteEsValida() {
        assertThat(validator.validate(EventFixtures.trackday(), null)).isEmpty();
    }

    @Test
    void terminoAntesDelInicioElMismoDiaSeMarcaEnEndTime() {
        Changes c = new Changes();
        c.endDate = LocalDate.of(2026, 10, 24);
        c.endTime = LocalTime.of(20, 0);

        List<FieldError> errors = validator.validate(with(EventFixtures.match(), c), null);

        assertThat(codesOf(errors, "endTime")).containsExactly("END_BEFORE_START");
        assertThat(codesOf(errors, "endDate")).isEmpty();
    }

    @Test
    void terminoEnUnaFechaAnteriorSeMarcaEnEndDate() {
        Changes c = new Changes();
        c.endDate = LocalDate.of(2026, 10, 23);

        assertThat(codesOf(validator.validate(with(EventFixtures.match(), c), null), "endDate"))
                .containsExactly("END_BEFORE_START");
    }

    @Test
    void partidoExigeDeporteYDelegado() {
        Changes c = new Changes();
        c.clearSport = true;
        c.clearDelegate = true;

        List<FieldError> errors = validator.validate(with(EventFixtures.match(), c), null);

        assertThat(codesOf(errors, "sportId")).containsExactly("REQUIRED");
        assertThat(codesOf(errors, "delegateOid")).containsExactly("REQUIRED");
    }

    @Test
    void partidoNoAdmiteElMismoEquipoDosVeces() {
        Changes c = new Changes();
        c.teamTwoId = 100L;

        assertThat(codesOf(validator.validate(with(EventFixtures.match(), c), null), "teamTwoId"))
                .containsExactly("SAME_TEAM");
    }

    @Test
    void partidoNoAdmiteOrganizador() {
        Changes c = new Changes();
        c.organizerInstitutionId = 1L;

        assertThat(codesOf(validator.validate(with(EventFixtures.match(), c), null), "organizerInstitutionId"))
                .containsExactly("ORGANIZER_NOT_ALLOWED");
    }

    @Test
    void otroTipoExigeOrganizadorYNoAdmiteEquipos() {
        Changes c = new Changes();
        c.type = EventType.MEETING; // el partido trae equipos y no trae organizador

        List<FieldError> errors = validator.validate(with(EventFixtures.match(), c), null);

        assertThat(codesOf(errors, "organizerInstitutionId")).containsExactly("REQUIRED");
        assertThat(codesOf(errors, "organizerCampusId")).containsExactly("REQUIRED");
        assertThat(codesOf(errors, "teamOneId")).containsExactly("MATCHUP_NOT_ALLOWED");
        assertThat(codesOf(errors, "teamTwoId")).containsExactly("MATCHUP_NOT_ALLOWED");
        assertThat(codesOf(errors, "sportId")).isEmpty();
    }

    @Test
    void delegadoYNombreVanJuntos() {
        Changes c = new Changes();
        c.clearDelegateName = true;

        assertThat(codesOf(validator.validate(with(EventFixtures.match(), c), null), "delegateDisplayName"))
                .containsExactly("REQUIRED");
    }

    @Test
    void enUnPutElInternalIdNoPuedeCambiar() {
        assertThat(codesOf(validator.validate(EventFixtures.match(), "EVT-OTRO"), "internalId"))
                .containsExactly("IMMUTABLE");
        assertThat(validator.validate(EventFixtures.match(), "EVT-42")).isEmpty();
    }
}
