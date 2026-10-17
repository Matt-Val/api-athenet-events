package com.athenet.events.service;

import com.athenet.events.dto.EventRequest;
import com.athenet.events.dto.EventResponse;
import com.athenet.events.dto.EventTypeResponse;
import com.athenet.events.exception.EventNotFoundException;
import com.athenet.events.exception.EventValidationException;
import com.athenet.events.exception.FieldError;
import com.athenet.events.exception.InternalIdTakenException;
import com.athenet.events.mapper.EventMapper;
import com.athenet.events.messaging.EventPublishedPublisher;
import com.athenet.events.model.Event;
import com.athenet.events.model.EventStatus;
import com.athenet.events.model.EventType;
import com.athenet.events.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventValidator eventValidator;
    private final EventPublishedPublisher eventPublishedPublisher;

    public EventService(EventRepository eventRepository, EventMapper eventMapper, EventValidator eventValidator,
            EventPublishedPublisher eventPublishedPublisher) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.eventValidator = eventValidator;
        this.eventPublishedPublisher = eventPublishedPublisher;
    }

    // CATALOGO ---------------------------------
    public List<EventTypeResponse> getEventTypes() {
        return Arrays.stream(EventType.values()).map(EventTypeResponse::from).toList();
    }

    // VISTA PÚBLICA -----------------------------------------------------------------

    public List<EventResponse> getAllPublishedEvents() {
        return toResponses(eventRepository.findByStatusOrderByEventDateAscStartTimeAsc(EventStatus.PUBLISHED));
    }

    /** Calendario: eventos publicados que ocurren (aunque sea en parte) entre hoy y hoy + daysAhead. */
    public Page<EventResponse> getUpcomingEvents(int daysAhead, EventStatus status, Pageable pageable) {
        LocalDate today = LocalDate.now();
        return eventRepository
                .findByStatusAndEventDateLessThanEqualAndEndDateGreaterThanEqual(status, today.plusDays(daysAhead), today, pageable)
                .map(eventMapper::toResponse);
    }

    public EventResponse getEventByInternalId(String internalId) {
        return eventMapper.toResponse(findByInternalId(internalId));
    }

    public Optional<EventResponse> getNextEvent() {
        return eventRepository
                .findFirstByStatusAndEventDateGreaterThanEqualOrderByEventDateAscStartTimeAsc(EventStatus.PUBLISHED, LocalDate.now())
                .map(eventMapper::toResponse);
    }

    public List<EventResponse> getNextTenEvents() {
        return toResponses(eventRepository
                .findTop10ByStatusAndEventDateGreaterThanEqualOrderByEventDateAscStartTimeAsc(EventStatus.PUBLISHED, LocalDate.now()));
    }





    // --- ADMIN y gestion interna -----------------------------------------------------------------------

    /** Todos los estados, incluidos DRAFT y CANCELLED. */
    public List<EventResponse> getAllEvents() {
        return toResponses(eventRepository.findAllByOrderByEventDateAscStartTimeAsc());
    }

    @Transactional
    public EventResponse createEvent(EventRequest request) {
        rejectIfInvalid(eventValidator.validate(request, null));
        if (eventRepository.existsByInternalId(request.internalId().trim())) {
            throw new InternalIdTakenException(request.internalId());
        }
        Event saved = eventRepository.save(eventMapper.toEntity(request));
        publishIfNewlyPublished(null, saved);
        return eventMapper.toResponse(saved);
    }

    @Transactional
    public EventResponse updateEvent(String internalId, EventRequest request) {
        Event existing = findByInternalId(internalId);
        rejectIfInvalid(eventValidator.validate(request, internalId));
        EventStatus previous = existing.getStatus();
        eventMapper.updateEntity(existing, request);
        Event saved = eventRepository.save(existing);
        publishIfNewlyPublished(previous, saved);
        return eventMapper.toResponse(saved);
    }

    @Transactional
    public void deleteEvent(String internalId) {
        eventRepository.delete(findByInternalId(internalId));
    }

    // ==========================================

    private Event findByInternalId(String internalId) {
        return eventRepository.findByInternalId(internalId)
                .orElseThrow(() -> new EventNotFoundException(internalId));
    }

    // evento.publicado para ms-notificaciones: solo cuando el evento pasa a PUBLISHED
    private void publishIfNewlyPublished(EventStatus previous, Event saved) {
        if (previous != EventStatus.PUBLISHED && saved.getStatus() == EventStatus.PUBLISHED) {
            eventPublishedPublisher.publish(saved);
        }
    }

    private static void rejectIfInvalid(List<FieldError> errors) {
        if (!errors.isEmpty()) {
            throw new EventValidationException(errors);
        }
    }

    private List<EventResponse> toResponses(List<Event> events) {
        return events.stream().map(eventMapper::toResponse).toList();
    }
}
