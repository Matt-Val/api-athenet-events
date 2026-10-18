package com.athenet.events;

import com.athenet.events.config.TestJwtDecoderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
    "spring.rabbitmq.listener.simple.auto-startup=false", "spring.rabbitmq.dynamic=false",
    "app.messaging.outbox-enabled=false"
})
@Import(TestJwtDecoderConfig.class)
@TestPropertySource(properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=")
class EventsApplicationTests {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.athenet.events.repository.EventRepository events;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.athenet.events.repository.OutboxMessageRepository outbox;

	@Test
	void contextLoads() {
	}

}
