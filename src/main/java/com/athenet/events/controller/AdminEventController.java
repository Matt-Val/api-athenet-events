package com.athenet.events.controller;


import com.athenet.events.model.Event;
import com.athenet.events.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {
    
    private final EventService eventService;

    public AdminEventController(EventService eventService) { 
        this.eventService = eventService;
    }

    // Endpoint para crear un nuevo evento
    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody Event event) { 
        Event createdEvent = eventService.createEvent(event);
        return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
    }

    // Endpoint para actualizar un evento existente usando su ID
    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @RequestBody Event event) { 
        return ResponseEntity.ok(eventService.updateEvent(id, event));
    }

    // Endpoint para elimnar un evento
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) { 
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build(); // 204 sin contenido
    }
}
