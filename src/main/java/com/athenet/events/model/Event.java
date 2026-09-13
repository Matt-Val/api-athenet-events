package com.athenet.events.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Event {
    
    // Uso interno en base de datos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Clave de negocio - ID que verá el usuario
    @NotBlank
    @Column(name = "internal_id", nullable = false, unique = true)
    private String internalId;

    // Datos generales del evento
    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Column(nullable = false)
    private String description;

    // Opcional para mas caracteres en la descripcion
    @Column(columnDefinition = "TEXT")
    private String description_opt;

    // Guarda la URL de la imagen del evento
    // Imagen principal
    @NotBlank
    @Column(name = "cover_image", nullable = false)
    private String coverImage;

    @ElementCollection(fetch = FetchType.LAZY) // Las fotos no se traen de la base hasta que alguien acceda.
    @CollectionTable(name = "event_photos", joinColumns = @JoinColumn (name = "event_id")) // Apunta al evento.
    @Column(name = "photo_url")
    private List<String> photos = new ArrayList<>();

    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category;

    // Fecha del evento - Se guarda como LocalDate para no tener problemas de zona horaria.
    @NotNull
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @JsonProperty("isOfficial")
    @Column(name = "is_official_flag", nullable = false)
    private boolean isOfficial;

    @NotNull
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    // Lugar del evento (ciudad, país, etc.)
    private String location;

    // Address del evento (calle, número, etc.)
    private String address;

    // Equipos participantes (si aplica)
    @Column(name = "team_one_id")
    private Long teamOneId;

    @Column(name = "team_two_id")
    private Long teamTwoId;
    
}
