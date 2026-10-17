package com.athenet.events.messaging;

import com.athenet.events.config.RabbitConfig;
import com.athenet.events.mapper.EventMapper;
import com.athenet.events.model.Event;
import com.athenet.events.model.EventStatus;
import com.athenet.events.support.EventFixtures;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EventPublishedPublisherTest {

    @Test
    void publicaElSobreConElEventoYSusFechasComoTexto() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        Event event = new EventMapper().toEntity(EventFixtures.match(EventStatus.PUBLISHED));

        new EventPublishedPublisher(new AfterCommitSender(template)).publish(event);

        var captor = ArgumentCaptor.forClass(EventPublishedEnvelope.class);
        verify(template).convertAndSend(eq(RabbitConfig.EXCHANGE), eq("evento.publicado"), captor.capture());
        JsonNode json = new ObjectMapper().valueToTree(captor.getValue());
        assertThat(json.get("type").asText()).isEqualTo("evento.publicado");
        assertThat(json.get("messageId").asText()).isNotBlank();
        assertThat(json.get("occurredAt").asText()).isNotBlank();
        JsonNode data = json.get("data");
        assertThat(data.get("internalId").asText()).isEqualTo("EVT-42");
        assertThat(data.get("title").asText()).isEqualTo("Clásico universitario");
        assertThat(data.get("type").asText()).isEqualTo("MATCH");
        assertThat(data.get("official").asBoolean()).isTrue();
        assertThat(data.get("eventDate").asText()).isEqualTo("2026-10-24");
        assertThat(data.get("startTime").asText()).isEqualTo("22:00");
        assertThat(data.get("endDate").asText()).isEqualTo("2026-10-25");
        assertThat(data.get("endTime").asText()).isEqualTo("00:30");
        assertThat(data.get("sportId").asLong()).isEqualTo(3L);
        assertThat(data.get("teamOneId").asLong()).isEqualTo(100L);
        assertThat(data.get("teamTwoId").asLong()).isEqualTo(200L);
        assertThat(data.get("delegateDisplayName").asText()).isEqualTo("María Pérez");
    }
}
