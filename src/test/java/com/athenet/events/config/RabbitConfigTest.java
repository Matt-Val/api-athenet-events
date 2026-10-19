package com.athenet.events.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigTest {

    @Test
    void publicaEnElExchangeTopicCompartidoYDurable() {
        TopicExchange exchange = new RabbitConfig().athenetTopicExchange();

        assertThat(exchange.getName()).isEqualTo("athenet.topic");
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }
}
