package com.athenet.events.service;

import com.athenet.events.dto.EventResponse;
import com.athenet.events.exception.EventNotFoundException;
import com.athenet.events.exception.EventValidationException;
import com.athenet.events.exception.InternalIdTakenException;
import com.athenet.events.mapper.EventMapper;
import com.athenet.events.messaging.EventPublishedPublisher;
import com.athenet.events.model.Event;
import com.athenet.events.model.EventStatus;
import com.athenet.events.repository.EventRepository;
import com.athenet.events.support.EventFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventServiceTest {

    private EventRepository repository;
    private EventPublishedPublisher publisher;
    private EventService service;
    private final EventMapper mapper = new EventMapper();

    @BeforeEach
    void setUp() {
        repository = mock(EventRepository.class);
        publisher = mock(EventPublishedPublisher.class);
        service = new EventService(repository, mapper, new EventValidator(), publisher);
        when(repository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void crearGuardaElEventoConLosCamposNuevos() {
        EventResponse created = service.createEvent(EventFixtures.match());

        assertThat(created.internalId()).isEqualTo("EVT-42");
        assertThat(created.endDate()).isEqualTo(java.time.LocalDate.of(2026, 10, 25));
        assertThat(created.endTime()).isEqualTo(LocalTime.of(0, 30));
        assertThat(created.delegateDisplayName()).isEqualTo("María Pérez");
        verify(repository).save(any(Event.class));
    }

    @Test
    void crearRechazaUnInternalIdRepetido() {
        when(repository.existsByInternalId("EVT-42")).thenReturn(true);

        assertThatThrownBy(() -> service.createEvent(EventFixtures.match()))
                .isInstanceOf(InternalIdTakenException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void crearNoGuardaSiIncumpleLasReglas() {
        var sinOrganizador = EventFixtures.trackday();
        var invalido = new com.athenet.events.dto.EventRequest(
                sinOrganizador.internalId(), sinOrganizador.title(), sinOrganizador.description(), null,
                sinOrganizador.type(), sinOrganizador.status(), false,
                sinOrganizador.eventDate(), sinOrganizador.startTime(), sinOrganizador.endDate(), sinOrganizador.endTime(),
                null, null, sinOrganizador.coverImage(), null,
                null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.createEvent(invalido))
                .isInstanceOf(EventValidationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void actualizarConservaElInternalIdYReemplazaLasFotos() {
        Event existing = mapper.toEntity(EventFixtures.match());
        existing.getPhotos().add("https://vieja.jpg");
        when(repository.findByInternalId("EVT-42")).thenReturn(Optional.of(existing));

        EventResponse updated = service.updateEvent("EVT-42", EventFixtures.match());

        assertThat(updated.internalId()).isEqualTo("EVT-42");
        assertThat(updated.photos()).isEmpty();
    }

    @Test
    void crearNotificaSoloSiNacePublicado() {
        service.createEvent(EventFixtures.match());
        verifyNoInteractions(publisher);

        service.createEvent(EventFixtures.match(EventStatus.PUBLISHED));
        verify(publisher).publish(any(Event.class));
    }

    @Test
    void actualizarNotificaSoloAlPasarAPublicado() {
        Event existing = mapper.toEntity(EventFixtures.match());
        when(repository.findByInternalId("EVT-42")).thenReturn(Optional.of(existing));

        service.updateEvent("EVT-42", EventFixtures.match(EventStatus.PUBLISHED));
        service.updateEvent("EVT-42", EventFixtures.match(EventStatus.PUBLISHED));

        verify(publisher, times(1)).publish(existing);
    }

    @Test
    void actualizarOEliminarUnEventoInexistenteEs404() {
        when(repository.findByInternalId("EVT-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateEvent("EVT-999", EventFixtures.match()))
                .isInstanceOf(EventNotFoundException.class);
        assertThatThrownBy(() -> service.deleteEvent("EVT-999"))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void elCatalogoDeTiposMarcaSoloElPartidoComoEnfrentamiento() {
        assertThat(service.getEventTypes())
                .filteredOn(t -> t.requiresMatchup())
                .extracting(t -> t.code())
                .containsExactly("MATCH");
    }
}
