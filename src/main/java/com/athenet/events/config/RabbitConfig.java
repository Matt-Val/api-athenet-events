package com.athenet.events.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Publicación en el exchange "athenet.topic", compartido entre los microservicios.
 * ms-events solo publica: cada consumidor (Notificaciones) declara su propia cola, binding y DLQ.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "athenet.topic";

    public static final String EVENT_PUBLISHED_ROUTING_KEY = "evento.publicado";

    // Jackson 2 propio para el converter, igual que en ms-competition
    @Bean
    ObjectMapper rabbitObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper rabbitObjectMapper) {
        return new Jackson2JsonMessageConverter(rabbitObjectMapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    TopicExchange athenetTopicExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }
}
