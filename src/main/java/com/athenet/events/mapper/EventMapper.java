package com.athenet.events.mapper;

import com.athenet.events.dto.EventRequest;
import com.athenet.events.dto.EventResponse;
import com.athenet.events.model.Event;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventMapper {

    public Event toEntity(EventRequest request) {
        Event event = new Event();
        event.setInternalId(request.internalId().trim());
        updateEntity(event, request);
        return event;
    }

    /** Copia todo salvo internalId, que es inmutable. */
    public void updateEntity(Event event, EventRequest request) {
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setDescriptionOpt(request.descriptionOpt());
        event.setType(request.type());
        event.setStatus(request.status());
        event.setOfficial(request.isOfficial());

        event.setEventDate(request.eventDate());
        event.setStartTime(request.startTime());
        event.setEndDate(request.endDate());
        event.setEndTime(request.endTime());

        event.setLocation(request.location());
        event.setAddress(request.address());
        event.setCoverImage(request.coverImage());
        event.getPhotos().clear();
        if (request.photos() != null) {
            event.getPhotos().addAll(request.photos());
        }

        event.setSportId(request.sportId());
        event.setOrganizerInstitutionId(request.organizerInstitutionId());
        event.setOrganizerCampusId(request.organizerCampusId());
        event.setOrganizerTeamId(request.organizerTeamId());
        event.setTeamOneId(request.teamOneId());
        event.setTeamTwoId(request.teamTwoId());

        event.setDelegateOid(request.delegateOid());
        event.setDelegateDisplayName(request.delegateDisplayName());
    }

    public EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getInternalId(),
                event.getTitle(),
                event.getDescription(),
                event.getDescriptionOpt(),
                event.getType(),
                event.getStatus(),
                event.isOfficial(),
                event.getEventDate(),
                event.getStartTime(),
                event.getEndDate(),
                event.getEndTime(),
                event.getLocation(),
                event.getAddress(),
                event.getCoverImage(),
                List.copyOf(event.getPhotos()),
                event.getSportId(),
                event.getOrganizerInstitutionId(),
                event.getOrganizerCampusId(),
                event.getOrganizerTeamId(),
                event.getTeamOneId(),
                event.getTeamTwoId(),
                event.getDelegateOid(),
                event.getDelegateDisplayName(), event.getCompetitionId(), event.getCompetitionMatchId()
        );
    }
}
