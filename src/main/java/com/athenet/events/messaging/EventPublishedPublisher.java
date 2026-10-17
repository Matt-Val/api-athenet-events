package com.athenet.events.messaging;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.athenet.events.config.RabbitConfig;
import com.athenet.events.model.Event;

/** Publica evento.publicado cuando un evento queda en PUBLISHED, al crearlo o al editarlo. */
@Component
public class EventPublishedPublisher {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AfterCommitSender sender;

    public EventPublishedPublisher(AfterCommitSender sender) {
        this.sender = sender;
    }

    public void publish(Event event) {
        EventPublishedMessage data = new EventPublishedMessage(
                event.getInternalId(),
                event.getTitle(),
                event.getDescription(),
                event.getType().name(),
                event.isOfficial(),
                event.getEventDate().toString(),
                event.getStartTime().format(TIME_FORMAT),
                event.getEndDate().toString(),
                event.getEndTime().format(TIME_FORMAT),
                event.getLocation(),
                event.getAddress(),
                event.getCoverImage(),
                event.getSportId(),
                event.getOrganizerInstitutionId(),
                event.getOrganizerCampusId(),
                event.getOrganizerTeamId(),
                event.getTeamOneId(),
                event.getTeamTwoId(),
                event.getDelegateOid(),
                event.getDelegateDisplayName());

        EventPublishedEnvelope envelope = new EventPublishedEnvelope(
                UUID.randomUUID().toString(),
                Instant.now().toString(),
                RabbitConfig.EVENT_PUBLISHED_ROUTING_KEY,
                data);

        sender.send(RabbitConfig.EVENT_PUBLISHED_ROUTING_KEY, envelope);
    }
}
