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

    private Event publishedTrackday(String internalId) {
        // Jornada del 6 al 8 de noviembre de 2026
        Event event = new EventMapper().toEntity(EventFixtures.trackday());
        event.setInternalId(internalId);
        event.setStatus(EventStatus.PUBLISHED);
        return repository.save(event);
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
}
