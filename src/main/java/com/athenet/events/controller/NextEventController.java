package com.athenet.events.controller;

import com.athenet.events.dto.NextEventResponse;
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
    public ResponseEntity<NextEventResponse> getNextEvent() {
        return eventService.getNextEvent()
                .map(event -> ResponseEntity.ok(new NextEventResponse(
                        event.getId(),
                        event.getTitle(),
                        event.getEventDate(),
                        event.getLocation(),
                        event.getCategory().name()
                )))
                .orElse(ResponseEntity.noContent().build());
    }
}
