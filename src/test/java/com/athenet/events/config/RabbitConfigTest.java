package com.athenet.events.config;

import com.athenet.events.messaging.MatchEnvelope;
import com.athenet.events.support.EventFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigTest {

    private final RabbitConfig config = new RabbitConfig();

    @Test
    void laDlqConservaLaRoutingKeyDelPartido() {
        assertThat(config.matchesQueue().getArguments()).containsEntry("x-dead-letter-exchange", "athenet.dlx")
                .doesNotContainKey("x-dead-letter-routing-key");
        assertThat(config.matchesBinding(config.matchesQueue(), config.athenetTopicExchange()).getRoutingKey())
                .isEqualTo("partido.actualizado");
        assertThat(config.matchesDlqBinding(config.matchesDlq(), config.deadLetterExchange()).getRoutingKey())
                .isEqualTo("partido.actualizado");
    }

    @Test
    void elMapperIgnoraCamposNuevosDelContrato() throws Exception {
        String conCampoNuevo = EventFixtures.COMPETITION_MATCH_JSON
                .replace("\"delegateDisplayName\": \"María Pérez\"", "\"delegateDisplayName\": \"María Pérez\", \"campoNuevo\": true");

        MatchEnvelope envelope = config.rabbitObjectMapper().readValue(conCampoNuevo, MatchEnvelope.class);

        assertThat(envelope.data().matchId()).isEqualTo(31L);
    }
}
