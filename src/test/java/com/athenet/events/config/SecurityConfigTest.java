package com.athenet.events.config;

import com.athenet.events.controller.AdminEventController;
import com.athenet.events.controller.PublicEventController;
import com.athenet.events.dto.EventRequest;
import com.athenet.events.dto.EventResponse;
import com.athenet.events.dto.EventTypeResponse;
import com.athenet.events.exception.EventNotFoundException;
import com.athenet.events.mapper.EventMapper;
import com.athenet.events.service.EventService;
import com.athenet.events.support.EventFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
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

    private static final EventResponse MATCH =
            new EventMapper().toResponse(new EventMapper().toEntity(EventFixtures.match()));

    // ── Rutas públicas ───────────────────────────────────────────────────────

    @Test
    void publicGetAllEventsWithoutTokenIsOk() throws Exception {
        when(eventService.getAllPublishedEvents()).thenReturn(List.of(MATCH));

        mockMvc.perform(get("/api/public/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sportId").value(3));
    }

    @Test
    void publicGetNextEventsWithoutTokenIsOk() throws Exception {
        when(eventService.getNextTenEvents()).thenReturn(List.of(MATCH));

        mockMvc.perform(get("/api/public/events/next"))
                .andExpect(status().isOk());
    }

    @Test
    void publicGetFeaturedEventWithoutTokenIsOk() throws Exception {
        when(eventService.getNextEvent()).thenReturn(Optional.of(MATCH));

        mockMvc.perform(get("/api/public/events/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOfficial").value(true))
                .andExpect(jsonPath("$.startTime").value("22:00"));
    }

    @Test
    void publicEventDetailWithoutTokenIsOk() throws Exception {
        when(eventService.getEventByInternalId(anyString())).thenReturn(MATCH);

        mockMvc.perform(get("/api/public/events/EVT-42"))
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
    void theOldNextEventAliasNoLongerExists() throws Exception {
        // GET /api/events/next se consolidó en /api/public/events/featured.
        mockMvc.perform(get("/api/events/next")).andExpect(status().isUnauthorized());
    }

    // ── Rutas de administración (rol DIRECTOR) ────────────────────────────────

    @Test
    void adminRoutesWithoutTokenAreUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/events")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EventFixtures.MATCH_JSON))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/admin/events/EVT-42")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminRoutesWithJwtWithoutDirectorRoleAreForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/events")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EventFixtures.MATCH_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/events/EVT-42").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void eventTypesRequireDirectorRole() throws Exception {
        when(eventService.getEventTypes()).thenReturn(List.of(new EventTypeResponse("MATCH", "Partido", true)));

        mockMvc.perform(get("/api/admin/event-types")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/event-types").with(jwt())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/event-types").with(jwt().authorities(DIRECTOR_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("MATCH"))
                .andExpect(jsonPath("$[0].requiresMatchup").value(true));
    }

    @Test
    void adminGetEventByInternalIdWithDirectorIsOk() throws Exception {
        when(eventService.getEventByInternalId("EVT-42")).thenReturn(MATCH);

        mockMvc.perform(get("/api/admin/events/EVT-42").with(jwt().authorities(DIRECTOR_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internalId").value("EVT-42"));
    }

    @Test
    void createEventWithDirectorReachesController() throws Exception {
        when(eventService.createEvent(any(EventRequest.class))).thenReturn(MATCH);

        mockMvc.perform(post("/api/admin/events")
                        .with(jwt().authorities(DIRECTOR_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EventFixtures.MATCH_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void updateEventByInternalIdWithDirectorIsOk() throws Exception {
        when(eventService.updateEvent(eq("EVT-42"), any(EventRequest.class))).thenReturn(MATCH);

        mockMvc.perform(put("/api/admin/events/EVT-42")
                        .with(jwt().authorities(DIRECTOR_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EventFixtures.MATCH_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void deleteEventByInternalIdWithDirectorIsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/events/EVT-42").with(jwt().authorities(DIRECTOR_ROLE)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteEventNotFoundReturns404() throws Exception {
        doThrow(new EventNotFoundException("EVT-999")).when(eventService).deleteEvent("EVT-999");

        mockMvc.perform(delete("/api/admin/events/EVT-999").with(jwt().authorities(DIRECTOR_ROLE)))
                .andExpect(status().isNotFound());
    }
}
