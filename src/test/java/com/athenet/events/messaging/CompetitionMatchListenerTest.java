package com.athenet.events.messaging;

import com.athenet.events.service.EventService;
import com.athenet.events.support.EventFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CompetitionMatchListenerTest {

    private final EventService eventService = mock(EventService.class);
    private final CompetitionMatchListener listener = new CompetitionMatchListener(eventService, new ObjectMapper());

    private static Message message(String json) {
        return new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }

    @Test
    void leeElMensajeCrudoYActualizaLaCopia() throws Exception {
        listener.onMatchMessage(message(EventFixtures.COMPETITION_MATCH_JSON));

        ArgumentCaptor<MatchMessage> captor = ArgumentCaptor.forClass(MatchMessage.class);
        verify(eventService).upsertFromCompetition(captor.capture());
        assertThat(captor.getValue().matchId()).isEqualTo(31L);
        assertThat(captor.getValue().time()).isEqualTo("18:00");
        assertThat(captor.getValue().homeTeamId()).isEqualTo(12L);
    }

    @Test
    void ignoraTiposDeMensajeDesconocidos() throws Exception {
        listener.onMatchMessage(message(EventFixtures.COMPETITION_MATCH_JSON
                .replace("\"type\": \"partido.actualizado\"", "\"type\": \"partido.otro\"")));

        verifyNoInteractions(eventService);
    }
}
