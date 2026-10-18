package com.athenet.events.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.athenet.events.messaging.*;
import com.athenet.events.model.*;
import com.athenet.events.repository.EventRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchProjectionService {
    private final EventRepository events;
    private final EventPublishedPublisher notices;

    @Transactional
    public void accept(MatchScheduleEnvelope.Data data) {
        if (data.homeTeamId().equals(data.awayTeamId())) throw new IllegalArgumentException("Equipos iguales");
        var date = LocalDate.parse(data.eventDate()); var start = LocalTime.parse(data.startTime());
        var endDate = LocalDate.parse(data.endDate()); var endTime = LocalTime.parse(data.endTime());
        if (!LocalDateTime.of(endDate,endTime).isAfter(LocalDateTime.of(date,start))) {
            throw new IllegalArgumentException("Término inválido");
        }
        var event = events.findLockedByCompetitionMatchId(data.matchId()).orElse(null);
        if (event != null && !event.getCompetitionId().equals(data.competitionId())) {
            throw new IllegalArgumentException("Origen de encuentro inconsistente");
        }
        if (event != null && event.getScheduleVersion() >= data.scheduleVersion()) return;
        EventNotificationData before = event == null ? null : EventNotificationData.from(event);
        if (event == null) {
            String internalId = "PARTIDO-" + data.matchId();
            if (events.existsByInternalId(internalId)) throw new IllegalArgumentException("ID de copia ocupado");
            event = new Event(); event.setInternalId(internalId);
            event.setCompetitionId(data.competitionId()); event.setCompetitionMatchId(data.matchId());
        }
        event.setTitle(data.title()); event.setDescription(data.description()); event.setCoverImage(data.coverImage());
        event.setType(EventType.MATCH); event.setStatus(EventStatus.valueOf(data.status())); event.setOfficial(true);
        event.setEventDate(date); event.setStartTime(start); event.setEndDate(endDate); event.setEndTime(endTime);
        event.setLocation(data.location()); event.setAddress(data.address()); event.setSportId(data.sportId());
        event.setTeamOneId(data.homeTeamId()); event.setTeamTwoId(data.awayTeamId());
        event.setDelegateOid(data.delegateOid()); event.setDelegateDisplayName(data.delegateDisplayName());
        event.setScheduleVersion(data.scheduleVersion());
        var saved = events.save(event);
        notices.changed(before,saved,false);
    }
}
