package com.athenet.events.controller;

import com.athenet.events.model.Event;
import com.athenet.events.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class NextEventController {

    private final EventService eventService;

    public NextEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/next")
    public ResponseEntity<Event> getNextEvent() {
        return eventService.getNextEvent()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
