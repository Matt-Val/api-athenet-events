package com.athenet.events.dto;

import java.time.LocalDate;

public record NextEventResponse(Long id, String title, LocalDate date, String location, String category) {}
