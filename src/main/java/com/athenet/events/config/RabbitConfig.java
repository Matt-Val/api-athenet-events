package com.athenet.events.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Cola de partidos de ms-competition. El exchange "athenet.topic" es compartido entre los
 * microservicios; el DLX "athenet.dlx" es topic para que cada DLQ reciba solo lo suyo.
 * Si el listener falla, se reintenta 3 veces y el mensaje termina en "eventos.partidos.dlq".
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "athenet.topic";
    public static final String DEAD_LETTER_EXCHANGE = "athenet.dlx";

    public static final String MATCHES_QUEUE = "eventos.partidos";
    public static final String MATCHES_DLQ = "eventos.partidos.dlq";
    public static final String MATCH_UPDATED_ROUTING_KEY = "partido.actualizado";

    /**
     * Jackson 2 propio para leer los mensajes, igual que en ms-competition: el listener
     * deserializa el body a mano y no depende del header de clase Java del publicador.
     * Ignora campos nuevos del contrato para no tumbar el consumo.
     */
    @Bean
    ObjectMapper rabbitObjectMapper() {
        return new ObjectMapper().findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // 3 reintentos (1s, 2s, 4s) y luego rechazo sin reencolar: entra el x-dead-letter-exchange de la cola
    @Bean
    StatelessRetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(3)
                .backOffOptions(1000, 2.0, 8000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            StatelessRetryOperationsInterceptor retryInterceptor,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(retryInterceptor);
        factory.setAutoStartup(autoStartup);
        return factory;
    }

    @Bean
    TopicExchange athenetTopicExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    TopicExchange deadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    // Sin x-dead-letter-routing-key: el mensaje muerto conserva su routing key y calza con el binding de la DLQ
    @Bean
    Queue matchesQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        return new Queue(MATCHES_QUEUE, true, false, false, args);
    }

    @Bean
    Queue matchesDlq() {
        return new Queue(MATCHES_DLQ, true);
    }

    @Bean
    Binding matchesBinding(Queue matchesQueue, TopicExchange athenetTopicExchange) {
        return BindingBuilder.bind(matchesQueue).to(athenetTopicExchange).with(MATCH_UPDATED_ROUTING_KEY);
    }

    @Bean
    Binding matchesDlqBinding(Queue matchesDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(matchesDlq).to(deadLetterExchange).with(MATCH_UPDATED_ROUTING_KEY);
    }
}
