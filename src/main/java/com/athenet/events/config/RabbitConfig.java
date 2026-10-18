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
 * Eventos declara su cola de agenda y publica avisos; Notificaciones declara su propia cola y DLQ.
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
    RabbitTemplate outboxRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        template.setUsePublisherConnection(true);
        return template;
    }

    @Bean
    org.springframework.amqp.core.Declarables scheduleDeclarations() {
        var exchange = new TopicExchange(EXCHANGE, true, false);
        var dlx = new TopicExchange("athenet.dlx", true, false);
        var queue = org.springframework.amqp.core.QueueBuilder.durable("eventos.partidos")
                .deadLetterExchange("athenet.dlx").deadLetterRoutingKey("eventos.partidos.dlq").build();
        var dlq = org.springframework.amqp.core.QueueBuilder.durable("eventos.partidos.dlq").build();
        return new org.springframework.amqp.core.Declarables(dlx,queue,dlq,
                org.springframework.amqp.core.BindingBuilder.bind(queue).to(exchange).with("partido.actualizado"),
                org.springframework.amqp.core.BindingBuilder.bind(dlq).to(dlx).with("eventos.partidos.dlq"));
    }

    @Bean
    org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory scheduleRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            @org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup) {
        var factory = new org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAutoStartup(autoStartup);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(org.springframework.amqp.rabbit.config.RetryInterceptorBuilder.stateless()
                .configureRetryPolicy(policy -> policy.maxRetries(3).delay(java.time.Duration.ofSeconds(1))
                        .multiplier(2).maxDelay(java.time.Duration.ofSeconds(4)))
                .recoverer(new org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer()).build());
        return factory;
    }

    @Bean
    TopicExchange athenetTopicExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }
}
