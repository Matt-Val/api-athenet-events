package com.athenet.events.controller;

import com.athenet.events.config.SecurityConfig;
import com.athenet.events.config.TestJwtDecoderConfig;
import com.athenet.events.mapper.EventMapper;
import com.athenet.events.messaging.EventPublishedPublisher;
import com.athenet.events.model.Event;
import com.athenet.events.repository.EventRepository;
import com.athenet.events.service.EventService;
import com.athenet.events.service.EventValidator;
import com.athenet.events.support.EventFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Formato de las respuestas que consume el BFF: errores por campo (422),
 * body ilegible (400), conflicto (409), inexistente (404) y el 201 con Location.
 * Usa el servicio, el mapper y el validador reales; solo el repositorio y el publicador son mock.
 */
@WebMvcTest(controllers = AdminEventController.class)
@Import({ SecurityConfig.class, TestJwtDecoderConfig.class, EventService.class, EventMapper.class, EventValidator.class })
@TestPropertySource(properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=")
class EventErrorsTest {

    private static final SimpleGrantedAuthority DIRECTOR_ROLE = new SimpleGrantedAuthority("ROLE_DIRECTOR");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventRepository repository;

    @MockitoBean
    private EventPublishedPublisher eventPublishedPublisher;

    @BeforeEach
    void setUp() {
        when(repository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private MockHttpServletRequestBuilder asDirector(MockHttpServletRequestBuilder request, String body) {
        return request.with(jwt().authorities(DIRECTOR_ROLE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    @Test
    void crearDevuelve201ConLocationYHorasEnFormatoHHmm() throws Exception {
        mockMvc.perform(asDirector(post("/api/admin/events"), EventFixtures.MATCH_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/admin/events/EVT-42"))
                .andExpect(jsonPath("$.startTime").value("22:00"))
                .andExpect(jsonPath("$.endTime").value("00:30"))
                .andExpect(jsonPath("$.endDate").value("2026-10-25"))
                .andExpect(jsonPath("$.isOfficial").value(true))
                .andExpect(jsonPath("$.delegateDisplayName").value("María Pérez"))
                .andExpect(jsonPath("$.category").doesNotExist());
    }

    @Test
    void partidoSinDelegadoEs422ConElErrorEnSuCampo() throws Exception {
        String sinDelegado = EventFixtures.MATCH_JSON
                .replace("\"delegateOid\": \"3f2b9c1e-5a7d-4c1b-9e2f-1d0a8b6c4e21\"", "\"delegateOid\": null")
                .replace("\"delegateDisplayName\": \"María Pérez\"", "\"delegateDisplayName\": null");

        mockMvc.perform(asDirector(post("/api/admin/events"), sinDelegado))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("delegateOid"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("REQUIRED"));
    }

    @Test
    void obligatorioFaltanteEs422Required() throws Exception {
        String sinTitulo = EventFixtures.MATCH_JSON.replace("\"title\": \"Clásico universitario\"", "\"title\": \"\"");

        mockMvc.perform(asDirector(post("/api/admin/events"), sinTitulo))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("REQUIRED"));
    }

    @Test
    void horaMalFormadaEs400() throws Exception {
        String horaInvalida = EventFixtures.MATCH_JSON.replace("\"startTime\": \"22:00\"", "\"startTime\": \"9am\"");

        mockMvc.perform(asDirector(post("/api/admin/events"), horaInvalida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("startTime"));
    }

    @Test
    void objetoAnidadoEs400() throws Exception {
        String anidado = EventFixtures.MATCH_JSON.replace("\"sportId\": 3,", "\"sport\": { \"id\": 3 },");

        mockMvc.perform(asDirector(post("/api/admin/events"), anidado))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sport"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("UNKNOWN_FIELD"));
    }

    @Test
    void internalIdRepetidoEs409EnSuCampo() throws Exception {
        when(repository.existsByInternalId("EVT-42")).thenReturn(true);

        mockMvc.perform(asDirector(post("/api/admin/events"), EventFixtures.MATCH_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("internalId"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("INTERNAL_ID_TAKEN"));
    }

    @Test
    void actualizarUnEventoInexistenteEs404() throws Exception {
        when(repository.findByInternalId("EVT-42")).thenReturn(Optional.empty());

        mockMvc.perform(asDirector(put("/api/admin/events/EVT-42"), EventFixtures.MATCH_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
    }
}
