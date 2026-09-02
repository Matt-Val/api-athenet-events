package com.athenet.events.Model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    @Column(name = "internal_id", nullable = false, unique = true)
    private String internalId;

    // Datos generales del evento
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;


    // Guarda la URL de la imagen del evento
    // Imagen principal
    @Column(name = "cover_image", nullable = false)
    private String coverImage;

    @ElementCollection(fetch = FetchType.LAZY) // Las fotos no se traen de la base hasta que alguien acceda.
    @CollectionTable(name = "event_photos", joinColumns = @JoinColumn (name = "event_id")) // Apunta al evento.
    @Column(name = "photo_url")
    private List<String> photos = new ArrayList<>();

    // Categoría del evento
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType category;

    // Fecha del evento - Se guarda como LocalDate para no tener problemas de zona horaria.
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;
    

    @Column(name = "is_official_flag", nullable = false)
    private boolean isOfficial;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    
    @Column(name = "team_one_id")
    private Long teamOneId;

    @Column(name = "team_two_id")
    private Long teamTwoId;
}
