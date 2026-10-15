package com.athenet.events;

import com.athenet.events.config.TestJwtDecoderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestJwtDecoderConfig.class)
@TestPropertySource(properties = {
		"spring.security.oauth2.resourceserver.jwt.issuer-uri=",
		// Sin broker en los tests: el listener no arranca y no se declaran colas
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"spring.rabbitmq.dynamic=false"
})
class EventsApplicationTests {

	@Test
	void contextLoads() {
	}

}
