package com.athenet.events.controller;

import com.athenet.events.dto.EventRequest;
import com.athenet.events.dto.EventResponse;
import com.athenet.events.dto.EventTypeResponse;
import com.athenet.events.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * CRUD de eventos y catálogo de tipos para el rol DIRECTOR. Su consumidor es el
 * BFF, que valida la integridad contra ms-institution y reenvía el body plano.
 * Los eventos se direccionan por internalId, la clave que ve la persona.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminEventController {

    private final EventService eventService;

    public AdminEventController(EventService eventService) {
        this.eventService = eventService;
    }

    // Catálogo de tipos: requiresMatchup decide si el evento lleva enfrentamiento
    // (dos equipos y delegado) u organizador. El BFF lo reenvía como GET /event-types.
    @GetMapping("/event-types")
    public ResponseEntity<List<EventTypeResponse>> getEventTypes() {
        return ResponseEntity.ok(eventService.getEventTypes());
    }

    // Listado completo (incluye DRAFT/CANCELLED, no solo lo público)
    @GetMapping("/events")
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    // Detalle en cualquier estado
    @GetMapping("/events/{internalId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable String internalId) {
        return ResponseEntity.ok(eventService.getEventByInternalId(internalId));
    }

    @PostMapping("/events")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {
        EventResponse created = eventService.createEvent(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{internalId}")
                .buildAndExpand(created.internalId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/events/{internalId}")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable String internalId,
                                                     @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(internalId, request));
    }

    @DeleteMapping("/events/{internalId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String internalId) {
        eventService.deleteEvent(internalId);
        return ResponseEntity.noContent().build(); // 204 sin contenido
    }
}
