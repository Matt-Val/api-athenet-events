package com.athenet.events.messaging;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.athenet.events.config.RabbitConfig;
import com.athenet.events.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Consume eventos.partidos: cada partido.actualizado crea o actualiza la copia del partido
 * en el calendario. Si falla, los reintentos y la DLQ los maneja el contenedor (RabbitConfig).
 */
@Component
public class CompetitionMatchListener {

    private static final Logger log = LoggerFactory.getLogger(CompetitionMatchListener.class);

    private final EventService eventService;
    private final ObjectMapper rabbitObjectMapper;

    public CompetitionMatchListener(EventService eventService, ObjectMapper rabbitObjectMapper) {
        this.eventService = eventService;
        this.rabbitObjectMapper = rabbitObjectMapper;
    }

    @RabbitListener(queues = RabbitConfig.MATCHES_QUEUE)
    public void onMatchMessage(Message message) throws IOException {
        MatchEnvelope envelope = rabbitObjectMapper.readValue(message.getBody(), MatchEnvelope.class);

        if (!RabbitConfig.MATCH_UPDATED_ROUTING_KEY.equals(envelope.type())) {
            log.warn("Tipo de mensaje desconocido en {}: {}", RabbitConfig.MATCHES_QUEUE, envelope.type());
            return;
        }
        eventService.upsertFromCompetition(envelope.data());
    }
}
