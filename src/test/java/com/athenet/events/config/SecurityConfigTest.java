package com.athenet.events.config;

import com.athenet.events.controller.AdminEventController;
import com.athenet.events.controller.PublicEventController;
import com.athenet.events.model.Event;
import com.athenet.events.service.EventService;
import com.athenet.events.exception.EventNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { AdminEventController.class, PublicEventController.class })
@Import({ SecurityConfig.class, TestJwtDecoderConfig.class })
@TestPropertySource(properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @Test
    void publicGetAllEventsWithoutTokenIsOk() throws Exception {
        when(eventService.getAllPublishedEvents()).thenReturn(List.of(new Event()));

        mockMvc.perform(get("/api/public/events"))
                .andExpect(status().isOk());
    }

    @Test
    void publicGetNextEventsWithoutTokenIsOk() throws Exception {
        when(eventService.getNextTenEvents()).thenReturn(List.of(new Event()));

        mockMvc.perform(get("/api/public/events/next"))
                .andExpect(status().isOk());
    }

    @Test
    void publicGetFeaturedEventWithoutTokenIsOk() throws Exception {
        Event event = new Event();
        event.setOfficial(true);
        when(eventService.getNextEvent()).thenReturn(Optional.of(event));

        mockMvc.perform(get("/api/public/events/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOfficial").value(true));
    }

    @Test
    void publicEventDetailWithoutTokenIsOk() throws Exception {
        Event event = new Event();
        event.setOfficial(true);
        when(eventService.getEventByInternalId(anyString())).thenReturn(event);

        mockMvc.perform(get("/api/public/events/EVT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOfficial").value(true));
    }

    @Test
    void publicEventDetailNotFoundReturns404() throws Exception {
        when(eventService.getEventByInternalId("EVT-NOTFOUND"))
                .thenThrow(new EventNotFoundException("EVT-NOTFOUND"));

        mockMvc.perform(get("/api/public/events/EVT-NOTFOUND"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEventWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createEventWithJwtReachesController() throws Exception {
        when(eventService.createEvent(any(Event.class))).thenReturn(new Event());

        // Event tiene @AllArgsConstructor con un campo primitivo (isOfficial), así que
        // Jackson necesita un JSON completo para poder instanciarlo - un "{}" vacío
        // falla en el parseo antes de llegar al controller.
        String validEventJson = """
                {
                  "internalId": "EVT-SEC-001",
                  "title": "Evento de prueba",
                  "description": "desc",
                  "coverImage": "https://ejemplo.com/imagen.jpg",
                  "type": "MEETING",
                  "category": "AJEDREZ",
                  "eventDate": "2026-12-01",
                  "status": "DRAFT",
                  "isOfficial": true,
                  "organizationId": 1
                }
                """;

        mockMvc.perform(post("/api/admin/events")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEventJson))
                .andExpect(status().isCreated());
    }
}
