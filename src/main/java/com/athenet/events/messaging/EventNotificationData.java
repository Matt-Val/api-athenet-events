package com.athenet.events.messaging;

import com.athenet.events.model.Event;

public record EventNotificationData(String internalId, String title, String description, String type,
        String status, boolean official, String eventDate, String startTime, String endDate, String endTime,
        String location, String address, String coverImage, Long sportId, Long teamOneId, Long teamTwoId) {
    public static EventNotificationData from(Event e) {
        return new EventNotificationData(e.getInternalId(),e.getTitle(),e.getDescription(),e.getType().name(),
                e.getStatus().name(),e.isOfficial(),e.getEventDate().toString(),e.getStartTime().toString(),
                e.getEndDate().toString(),e.getEndTime().toString(),e.getLocation(),e.getAddress(),
                e.getCoverImage(),e.getSportId(),e.getTeamOneId(),e.getTeamTwoId());
    }
    public EventNotificationData cancelled() {
        return new EventNotificationData(internalId,title,description,type,"CANCELLED",official,
                eventDate,startTime,endDate,endTime,location,address,coverImage,sportId,teamOneId,teamTwoId);
    }
}
