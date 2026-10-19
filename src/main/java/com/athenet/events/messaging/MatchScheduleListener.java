package com.athenet.events.messaging;

import java.io.IOException;
import java.time.Instant;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.athenet.events.service.MatchProjectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MatchScheduleListener {
    private final ObjectMapper rabbitObjectMapper;
    private final Validator validator;
    private final MatchProjectionService projections;

    @RabbitListener(queues = "eventos.partidos", containerFactory = "scheduleRabbitListenerContainerFactory")
    public void receive(Message message) throws IOException {
        var envelope = rabbitObjectMapper.readValue(message.getBody(),MatchScheduleEnvelope.class);
        if (envelope == null || !validator.validate(envelope).isEmpty()) {
            throw new IllegalArgumentException("Contrato de agenda inválido");
        }
        Instant.parse(envelope.occurredAt());
        projections.accept(envelope.data());
    }
}
