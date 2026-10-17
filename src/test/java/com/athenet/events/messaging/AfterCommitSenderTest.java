package com.athenet.events.messaging;

import com.athenet.events.config.RabbitConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AfterCommitSenderTest {

    private final RabbitTemplate template = mock(RabbitTemplate.class);
    private final AfterCommitSender sender = new AfterCommitSender(template);

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void enviaSoloDespuesDelCommit() {
        TransactionSynchronizationManager.initSynchronization();

        sender.send("evento.publicado", "envelope");
        verifyNoInteractions(template);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(template).convertAndSend(RabbitConfig.EXCHANGE, "evento.publicado", "envelope");
    }

    @Test
    void unRollbackNoEnvia() {
        TransactionSynchronizationManager.initSynchronization();

        sender.send("evento.publicado", "envelope");
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(template);
    }

    @Test
    void sinTransaccionEnviaAlTiro() {
        sender.send("evento.publicado", "envelope");

        verify(template).convertAndSend(RabbitConfig.EXCHANGE, "evento.publicado", "envelope");
    }

    @Test
    void unBrokerCaidoDespuesDelCommitNoRompeLaRequest() {
        doThrow(new AmqpConnectException(new RuntimeException("broker caído")))
                .when(template).convertAndSend(any(String.class), any(String.class), any(Object.class));
        TransactionSynchronizationManager.initSynchronization();

        sender.send("evento.publicado", "envelope");

        assertThatCode(() -> TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit)).doesNotThrowAnyException();
    }
}
