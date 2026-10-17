package com.athenet.events.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.athenet.events.config.RabbitConfig;

/**
 * Envía a athenet.topic recién cuando la transacción en curso hace commit: si se revierte,
 * el mensaje no sale. Si el broker falla después del commit, el cambio ya quedó guardado:
 * se registra el error y el mensaje se pierde (no hay outbox).
 */
@Component
public class AfterCommitSender {

    private static final Logger log = LoggerFactory.getLogger(AfterCommitSender.class);

    private final RabbitTemplate rabbitTemplate;

    public AfterCommitSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(String routingKey, Object envelope) {
        Runnable publish = () -> convertAndSend(routingKey, envelope);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish.run();
            }
        });
    }

    private void convertAndSend(String routingKey, Object envelope) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, envelope);
        } catch (AmqpException ex) {
            log.error("No se pudo publicar {} en {}", routingKey, RabbitConfig.EXCHANGE, ex);
        }
    }
}
