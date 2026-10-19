package com.athenet.events.messaging;

import com.athenet.events.mapper.EventMapper;
import com.athenet.events.model.EventStatus;
import com.athenet.events.repository.OutboxMessageRepository;
import com.athenet.events.support.EventFixtures;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EventPublishedPublisherTest {
    final OutboxEventWriter writer = mock(OutboxEventWriter.class);
    final OutboxMessageRepository messages = mock(OutboxMessageRepository.class);
    final EventPublishedPublisher publisher = new EventPublishedPublisher(writer,messages);
    @Test void publicationModificationCancellationAndRepublicationHaveIncreasingVersions() {
        var event = new EventMapper().toEntity(EventFixtures.match(EventStatus.PUBLISHED));
        when(messages.lastRevision("EVT-42")).thenReturn(0L,1L,2L,3L);
        publisher.changed(null,event,false);
        var before = EventNotificationData.from(event);
        event.setLocation("Otro estadio");
        publisher.changed(before,event,false);
        publisher.changed(EventNotificationData.from(event),event,true);
        publisher.changed(null,event,false); // mismo internalId luego de borrado/recreación
        var notices = ArgumentCaptor.forClass(EventNotificationEnvelope.class);
        verify(writer,times(4)).enqueue(anyString(),eq("EVT-42"),anyLong(),notices.capture());
        assertThat(notices.getAllValues()).extracting(EventNotificationEnvelope::type)
                .containsExactly("evento.publicado","evento.modificado","evento.cancelado","evento.publicado");
        assertThat(notices.getAllValues()).extracting(EventNotificationEnvelope::version).containsExactly(1L,2L,3L,4L);
        assertThat(notices.getAllValues().get(2).data().status()).isEqualTo("CANCELLED");
        assertThat(notices.getAllValues().get(0).data().endDate()).isEqualTo("2026-10-25");
    }
    @Test void draftAndIdenticalPublishedSaveDoNotQueueAnotherNotice() {
        var event = new EventMapper().toEntity(EventFixtures.match());
        publisher.changed(null,event,false);
        event.setStatus(EventStatus.PUBLISHED);
        publisher.changed(EventNotificationData.from(event),event,false);
        verifyNoInteractions(writer,messages);
    }
}
