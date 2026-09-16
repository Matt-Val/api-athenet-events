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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    private static final SimpleGrantedAuthority DIRECTOR_ROLE = new SimpleGrantedAuthority("ROLE_DIRECTOR");

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
    void createEventWithJwtWithoutDirectorRoleIsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/events")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void createEventWithJwtReachesController() throws Exception {
        when(eventService.createEvent(any(Event.class))).thenReturn(new Event());

        mockMvc.perform(post("/api/admin/events")
                        .with(jwt().authorities(DIRECTOR_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT_JSON))
                .andExpect(status().isCreated());
    }

    // Event tiene @AllArgsConstructor con un campo primitivo (isOfficial), así que
    // Jackson necesita un JSON completo para poder instanciarlo - un "{}" vacío
    // falla en el parseo antes de llegar al controller.
    private static final String VALID_EVENT_JSON = """
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

    @Test
    void createEventWithInvalidPayloadReturns400() throws Exception {
        String missingRequiredFields = """
                {
                  "title": "Evento incompleto"
                }
                """;

        mockMvc.perform(post("/api/admin/events")
                        .with(jwt().authorities(DIRECTOR_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingRequiredFields))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEventWithDuplicateInternalIdReturns409() throws Exception {
        when(eventService.createEvent(any(Event.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        mockMvc.perform(post("/api/admin/events")
                        .with(jwt().authorities(DIRECTOR_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void updateEventWithJwtIsOk() throws Exception {
        when(eventService.updateEvent(anyLong(), any(Event.class))).thenReturn(new Event());

        mockMvc.perform(put("/api/admin/events/1")
                        .with(jwt().authorities(DIRECTOR_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void updateEventNotFoundReturns404() throws Exception {
        when(eventService.updateEvent(anyLong(), any(Event.class)))
                .thenThrow(new EventNotFoundException(999L));

        mockMvc.perform(put("/api/admin/events/999")
                        .with(jwt().authorities(DIRECTOR_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteEventWithJwtIsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/events/1").with(jwt().authorities(DIRECTOR_ROLE)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteEventNotFoundReturns404() throws Exception {
        doThrow(new EventNotFoundException(999L)).when(eventService).deleteEvent(999L);

        mockMvc.perform(delete("/api/admin/events/999").with(jwt().authorities(DIRECTOR_ROLE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteEventWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/admin/events/1"))
                .andExpect(status().isUnauthorized());
    }
}
