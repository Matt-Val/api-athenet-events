package com.athenet.events.controller;

import com.athenet.events.model.Event;
import com.athenet.events.model.EventStatus;
import com.athenet.events.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/events")
public class PublicEventController {

    private final EventService eventService;

    public PublicEventController(EventService eventService){
        this.eventService = eventService;
    }

    // Endpoint para listar todos los eventos publicados (catálogo de eventos)
    @GetMapping
    public ResponseEntity<List<Event>> getAllPublishedEvents() {
        return ResponseEntity.ok(eventService.getAllPublishedEvents());
    }

    // Endpoint para listar los próximos eventos publicados (Top 10, del más cercano al más lejano)
    @GetMapping("/next")
    public ResponseEntity<List<Event>> getNextTenEvents() {
        return ResponseEntity.ok(eventService.getNextTenEvents());
    }

    // Endpoint para obtener el evento destacado / individual más próximo para el hero countdown
    @GetMapping("/featured")
    public ResponseEntity<Event> getFeaturedNextEvent() {
        return eventService.getNextEvent()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // Endpoint para el calendario
    @GetMapping("/upcoming")
    public ResponseEntity<Page<Event>> getUpcomingEvents(
            @RequestParam(defaultValue = "30", required = false) int daysAhead,
            @PageableDefault(size = 10, sort = "eventDate") Pageable pageable) {
        
        Page<Event> eventsPage = eventService.getUpcomingEvents(daysAhead, EventStatus.PUBLISHED, pageable);
        return ResponseEntity.ok(eventsPage);
    }

    // Endpoint para ver el detalle de un evento específico usando su internalId
    @GetMapping("/{internalId}")
    public ResponseEntity<Event> getEventByInternalId(@PathVariable String internalId) { 
        return ResponseEntity.ok(eventService.getEventByInternalId(internalId));
    }

}
