package com.athenet.events.messaging;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.athenet.events.model.Event;
import com.athenet.events.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;

/** Avisos versionados persistidos en la transacción del evento; no envía al broker directamente. */
@Component
@RequiredArgsConstructor
public class EventPublishedPublisher {
    private final OutboxEventWriter outbox;
    private final OutboxMessageRepository messages;

    public void changed(EventNotificationData before, Event event, boolean deleted) {
        var after = EventNotificationData.from(event);
        boolean wasPublished = before != null && "PUBLISHED".equals(before.status());
        boolean isPublished = !deleted && "PUBLISHED".equals(after.status());
        String type;
        if (wasPublished && !isPublished) type = "evento.cancelado";
        else if (isPublished && !wasPublished) type = "evento.publicado";
        else if (isPublished && !after.equals(before)) type = "evento.modificado";
        else return;
        long version = messages.lastRevision(event.getInternalId()) + 1;
        event.setNotificationVersion(version);
        if (type.equals("evento.cancelado")) after = after.cancelled();
        outbox.enqueue(type,event.getInternalId(),version,
                new EventNotificationEnvelope(UUID.randomUUID().toString(),Instant.now().toString(),type,version,after));
    }
}
