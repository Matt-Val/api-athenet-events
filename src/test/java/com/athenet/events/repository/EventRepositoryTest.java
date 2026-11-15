package com.athenet.events.repository;

import com.athenet.events.mapper.EventMapper;
import com.athenet.events.model.Event;
import com.athenet.events.model.EventStatus;
import com.athenet.events.support.EventFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corre contra el Postgres configurado (el del job de CI o la base local
 * athenet_events_db), igual que EventsApplicationTests: no hay base embebida.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventRepositoryTest {

    @Autowired
    private EventRepository repository;

    // Jornada del 6 al 8 de noviembre de 2026 (09:00 a 18:00)
    private Event trackday(String internalId, EventStatus status) {
        Event event = new EventMapper().toEntity(EventFixtures.trackday());
        event.setInternalId(internalId);
        event.setStatus(status);
        return repository.save(event);
    }

    private Event publishedTrackday(String internalId) {
        return trackday(internalId, EventStatus.PUBLISHED);
    }

    @Test
    void elCalendarioIncluyeUnEventoDeVariosDiasQueYaEmpezo() {
        publishedTrackday("EVT-TEST-RANGO");

        // Consultado el día 7: el evento empezó el 6 pero sigue en curso.
        var page = repository.findByStatusAndEventDateLessThanEqualAndEndDateGreaterThanEqual(
                EventStatus.PUBLISHED, LocalDate.of(2026, 12, 7), LocalDate.of(2026, 11, 7), Pageable.unpaged());

        assertThat(page.getContent()).extracting(Event::getInternalId).contains("EVT-TEST-RANGO");
    }

    @Test
    void elCalendarioExcluyeUnEventoQueYaTermino() {
        publishedTrackday("EVT-TEST-PASADO");

        var page = repository.findByStatusAndEventDateLessThanEqualAndEndDateGreaterThanEqual(
                EventStatus.PUBLISHED, LocalDate.of(2026, 12, 9), LocalDate.of(2026, 11, 9), Pageable.unpaged());

        assertThat(page.getContent()).extracting(Event::getInternalId).doesNotContain("EVT-TEST-PASADO");
    }

    @Test
    void existsByInternalIdDetectaDuplicados() {
        publishedTrackday("EVT-TEST-DUP");

        assertThat(repository.existsByInternalId("EVT-TEST-DUP")).isTrue();
        assertThat(repository.existsByInternalId("EVT-NO-EXISTE")).isFalse();
    }

    // --- Listado vigente: publicados que todavia no terminaron (GET /events/all-next) ---

    @Test
    void elListadoVigenteIncluyeUnEventoEnCurso() {
        publishedTrackday("EVT-TEST-EN-CURSO");

        // Dia 7 al mediodia: empezo el 6 y termina el 8. Es el caso de EVT-2.
        var events = repository.findNotFinishedByStatusOrderByEventDateAscStartTimeAsc(
                EventStatus.PUBLISHED, LocalDate.of(2026, 11, 7), LocalTime.of(12, 0));

        assertThat(events).extracting(Event::getInternalId).contains("EVT-TEST-EN-CURSO");
    }

    @Test
    void elListadoVigenteExcluyeUnEventoQueYaTermino() {
        publishedTrackday("EVT-TEST-TERMINADO");

        // Dia 9: el evento termino el 8 a las 18:00.
        var events = repository.findNotFinishedByStatusOrderByEventDateAscStartTimeAsc(
                EventStatus.PUBLISHED, LocalDate.of(2026, 11, 9), LocalTime.of(12, 0));

        assertThat(events).extracting(Event::getInternalId).doesNotContain("EVT-TEST-TERMINADO");
    }

    @Test
    void elListadoVigenteIncluyeUnEventoQueTerminaHoyMasTarde() {
        publishedTrackday("EVT-TEST-TERMINA-HOY");

        // Mismo dia del termino (8), pero aun no son las 18:00.
        var events = repository.findNotFinishedByStatusOrderByEventDateAscStartTimeAsc(
                EventStatus.PUBLISHED, LocalDate.of(2026, 11, 8), LocalTime.of(17, 0));

        assertThat(events).extracting(Event::getInternalId).contains("EVT-TEST-TERMINA-HOY");
    }

    @Test
    void elListadoVigenteExcluyeUnEventoQueTerminoHoyMasTemprano() {
        publishedTrackday("EVT-TEST-TERMINO-HOY");

        // Mismo dia del termino (8) y ya pasaron las 18:00. Comparando solo por dia,
        // este caso daria el mismo resultado que el anterior: es lo que justifica
        // que la query mire tambien la hora.
        var events = repository.findNotFinishedByStatusOrderByEventDateAscStartTimeAsc(
                EventStatus.PUBLISHED, LocalDate.of(2026, 11, 8), LocalTime.of(19, 0));

        assertThat(events).extracting(Event::getInternalId).doesNotContain("EVT-TEST-TERMINO-HOY");
    }

    @Test
    void elListadoVigenteRespetaElStatus() {
        trackday("EVT-TEST-BORRADOR", EventStatus.DRAFT);

        var events = repository.findNotFinishedByStatusOrderByEventDateAscStartTimeAsc(
                EventStatus.PUBLISHED, LocalDate.of(2026, 11, 7), LocalTime.of(12, 0));

        assertThat(events).extracting(Event::getInternalId).doesNotContain("EVT-TEST-BORRADOR");
    }
}
