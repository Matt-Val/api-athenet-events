package com.athenet.events.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento institucional. Las referencias a ms-institution (deporte, institución, sede, equipo) 
 * se guardan como IDs sueltos: este microservicio no conoce esa base, así que su integridad la valida 
 * el BFF. 
 * Las reglas propias del evento (obligatorios, tipo, término posterior al inicio) viven en su validador EventValidator.
 */
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_events_status_date", columnList = "status, event_date"),
        @Index(name = "idx_events_sport", columnList = "sport_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    
    // Uso interno en base de datos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Clave de negocio - ID que verá el usuario y con el que se direcciona la API
    @Column(name = "internal_id", nullable = false, unique = true)
    private String internalId;

    // Datos generales del evento
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    // Opcional para mas caracteres en la descripcion
    @Column(name = "description_opt", columnDefinition = "TEXT")
    private String descriptionOpt;

    // Imagen principal
    @Column(name = "cover_image", nullable = false)
    private String coverImage;

    @ElementCollection(fetch = FetchType.EAGER) // Se traen junto con el evento para evitar LazyInitializationException al serializar.
    @CollectionTable(name = "event_photos", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "photo_url")
    private List<String> photos = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(name = "is_official_flag", nullable = false)
    private boolean isOfficial;

    // Inicio y término - LocalDate/LocalTime para no tener problemas de zona horaria.
    // Un evento puede durar varios días.
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // Lugar del evento (ciudad, país, etc.)
    private String location;

    // Address del evento (calle, número, etc.)
    private String address;

    // Deporte del catálogo de ms-institution (Deporte.id). Opcional salvo en partidos.
    @Column(name = "sport_id")
    private Long sportId;

    // Organizador: solo en eventos que no son partido. El equipo es opcional.
    @Column(name = "organizer_institution_id")
    private Long organizerInstitutionId;

    @Column(name = "organizer_campus_id")
    private Long organizerCampusId;

    @Column(name = "organizer_team_id")
    private Long organizerTeamId;

    // Equipos participantes (si aplica)
    @Column(name = "team_one_id")
    private Long teamOneId;

    @Column(name = "team_two_id")
    private Long teamTwoId;

    // Delegado (Entra ID). El nombre es un snapshot que aporta para la identificacion
    @Column(name = "delegate_oid", length = 36)
    private String delegateOid;

    @Column(name = "delegate_display_name", length = 256)
    private String delegateDisplayName;

    // Copia de un partido de ms-competition (partido.actualizado). Con competitionMatchId
    // el evento es de solo lectura: se edita en la competencia.
    @Column(name = "competition_id")
    private Long competitionId;

    @Column(name = "competition_match_id", unique = true)
    private Long competitionMatchId;
}
