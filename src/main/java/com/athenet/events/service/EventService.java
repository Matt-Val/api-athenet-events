package com.athenet.events.service;

import com.athenet.events.dto.EventRequest;
import com.athenet.events.dto.EventResponse;
import com.athenet.events.dto.EventTypeResponse;
import com.athenet.events.exception.EventNotFoundException;
import com.athenet.events.exception.EventValidationException;
import com.athenet.events.exception.FieldError;
import com.athenet.events.exception.InternalIdTakenException;
import com.athenet.events.exception.ManagedByCompetitionException;
import com.athenet.events.mapper.EventMapper;
import com.athenet.events.messaging.MatchMessage;
import com.athenet.events.model.Event;
import com.athenet.events.model.EventStatus;
import com.athenet.events.model.EventType;
import com.athenet.events.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private static final String COMPETITION_MATCH_PREFIX = "PARTIDO-";
    private static final Duration COMPETITION_MATCH_DURATION = Duration.ofHours(2);

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventValidator eventValidator;

    public EventService(EventRepository eventRepository, EventMapper eventMapper, EventValidator eventValidator) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.eventValidator = eventValidator;
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
        return eventMapper.toResponse(eventRepository.save(eventMapper.toEntity(request)));
    }

    @Transactional
    public EventResponse updateEvent(String internalId, EventRequest request) {
        Event existing = findByInternalId(internalId);
        rejectIfManagedByCompetition(existing);
        rejectIfInvalid(eventValidator.validate(request, internalId));
        eventMapper.updateEntity(existing, request);
        return eventMapper.toResponse(eventRepository.save(existing));
    }

    @Transactional
    public void deleteEvent(String internalId) {
        Event existing = findByInternalId(internalId);
        rejectIfManagedByCompetition(existing);
        eventRepository.delete(existing);
    }

    // --- Copias de partidos de ms-competition ----------------------------------------------------------

    /**
     * partido.actualizado: crea o actualiza la copia del partido, buscándola por competitionMatchId.
     * Nace publicada, con la imagen de la competencia como portada y 2 horas de duración. No pasa por
     * EventValidator: la competencia ya validó el partido.
     */
    @Transactional
    public void upsertFromCompetition(MatchMessage match) {
        Event event = eventRepository.findByCompetitionMatchId(match.matchId()).orElseGet(() -> {
            Event created = new Event();
            created.setInternalId(COMPETITION_MATCH_PREFIX + match.matchId());
            created.setCompetitionMatchId(match.matchId());
            return created;
        });
        LocalDateTime start = LocalDateTime.of(LocalDate.parse(match.date()), LocalTime.parse(match.time()));
        LocalDateTime end = start.plus(COMPETITION_MATCH_DURATION);

        event.setCompetitionId(match.competitionId());
        event.setTitle(match.title());
        event.setDescription(match.description());
        event.setType(EventType.MATCH);
        event.setStatus(EventStatus.PUBLISHED);
        event.setOfficial(true);
        event.setEventDate(start.toLocalDate());
        event.setStartTime(start.toLocalTime());
        event.setEndDate(end.toLocalDate());
        event.setEndTime(end.toLocalTime());
        event.setLocation(match.location());
        event.setAddress(match.address());
        event.setCoverImage(match.competitionImg());
        event.setSportId(match.sportId());
        event.setTeamOneId(match.homeTeamId());
        event.setTeamTwoId(match.awayTeamId());
        event.setDelegateOid(match.delegateOid());
        event.setDelegateDisplayName(match.delegateDisplayName());
        eventRepository.save(event);
    }

    // ==========================================

    private Event findByInternalId(String internalId) {
        return eventRepository.findByInternalId(internalId)
                .orElseThrow(() -> new EventNotFoundException(internalId));
    }

    private static void rejectIfManagedByCompetition(Event event) {
        if (event.getCompetitionMatchId() != null) {
            throw new ManagedByCompetitionException(event.getInternalId());
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
