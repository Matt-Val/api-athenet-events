package com.athenet.events.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.athenet.events.messaging.*;
import com.athenet.events.model.*;
import com.athenet.events.repository.EventRepository;
import com.athenet.events.mapper.EventMapper;
import com.athenet.events.support.EventFixtures;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MatchProjectionServiceTest {
    final EventRepository events = mock(EventRepository.class);
    final EventPublishedPublisher notices = mock(EventPublishedPublisher.class);
    final MatchProjectionService service = new MatchProjectionService(events,notices);
    MatchScheduleEnvelope.Data data(int revision, String status) {
        return new MatchScheduleEnvelope.Data(31L,4L,3L,revision,"Halcones vs Pumas","Copa — Octavos",
                "https://example.test/cup.png","2026-10-24","22:00","2026-10-25","00:30","Estadio",null,
                12L,15L,EventFixtures.DELEGATE_OID,"Delegado",status);
    }
    @Test void createsCopyAndIgnoresDuplicatesAndOlderRevisions() {
        when(events.save(any(Event.class))).thenAnswer(call -> call.getArgument(0));
        service.accept(data(2,"PUBLISHED"));
        var captor = org.mockito.ArgumentCaptor.forClass(Event.class);
        verify(events).save(captor.capture());
        var created = captor.getValue();
        assertThat(created.getInternalId()).isEqualTo("PARTIDO-31");
        assertThat(created.getCompetitionId()).isEqualTo(4L);
        assertThat(created.getCompetitionMatchId()).isEqualTo(31L);
        assertThat(created.getEndDate()).isEqualTo(java.time.LocalDate.of(2026,10,25));
        when(events.findLockedByCompetitionMatchId(31L)).thenReturn(Optional.of(created));
        service.accept(data(2,"PUBLISHED")); service.accept(data(1,"PUBLISHED"));
        verify(events,times(1)).save(any());
        verify(notices,times(1)).changed(any(),any(),eq(false));
    }
    @Test void cancellationBeforeCreationLeavesTombstoneAgainstLatePublication() {
        when(events.save(any(Event.class))).thenAnswer(call -> call.getArgument(0));
        service.accept(data(3,"CANCELLED"));
        var captor = org.mockito.ArgumentCaptor.forClass(Event.class);
        verify(events).save(captor.capture());
        var created = captor.getValue();
        assertThat(created.getStatus()).isEqualTo(EventStatus.CANCELLED);
        when(events.findLockedByCompetitionMatchId(31L)).thenReturn(Optional.of(created));
        service.accept(data(2,"PUBLISHED"));
        assertThat(created.getStatus()).isEqualTo(EventStatus.CANCELLED);
        verify(events,times(1)).save(any());
    }
    @Test void occupiedInternalIdIsNotOverwritten() {
        when(events.existsByInternalId("PARTIDO-31")).thenReturn(true);
        assertThatThrownBy(() -> service.accept(data(1,"PUBLISHED"))).isInstanceOf(IllegalArgumentException.class);
        verify(events,never()).save(any());
    }
    @Test void copiesAreReadOnlyThroughEventCrudEvenWhenRequestChangesType() {
        var existing = new EventMapper().toEntity(EventFixtures.match(EventStatus.PUBLISHED));
        existing.setCompetitionId(4L); existing.setCompetitionMatchId(31L);
        when(events.findByInternalId("EVT-42")).thenReturn(Optional.of(existing));
        var crud = new EventService(events,new EventMapper(),new EventValidator(),notices);
        assertThatThrownBy(() -> crud.updateEvent("EVT-42",EventFixtures.trackday()))
                .isInstanceOf(com.athenet.events.exception.CompetitionEventReadOnlyException.class);
        assertThatThrownBy(() -> crud.deleteEvent("EVT-42"))
                .isInstanceOf(com.athenet.events.exception.CompetitionEventReadOnlyException.class);
        verify(events,never()).delete(any()); verify(events,never()).save(any());
    }
}
