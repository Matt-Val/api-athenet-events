package com.athenet.events.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.athenet.events.model.Event;
import com.athenet.events.model.EventStatus;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /*  CRUD (Create, Read, Update, Delete):
        • save(S entity): Guarda una entidad.
        • findById(ID id): Encuentra una entidad por su ID.
        • existsById(ID id): Verifica si una entidad con un ID dado existe.
        • findAll(): Encuentra todas las entidades.
        • findAllById(Iterable<ID> ids): Encuentra todas las entidades por sus IDs.
        • count(): Cuenta todas las entidades.
        • deleteById(ID id): Borra una entidad por su ID.
        • delete(S entity): Borra una entidad.
        • deleteAll(): Borra todas las entidades.

    Paginación y Ordenación:
        • findAll(Pageable pageable): Encuentra todas las entidades con paginación.
        • findAll(Sort sort): Encuentra todas las entidades con ordenación.
    */

    Optional<Event> findByInternalId(String internalId);

    boolean existsByInternalId(String internalId);

    // Copia de un partido de ms-competition: upsert por el id del partido
    Optional<Event> findByCompetitionMatchId(Long competitionMatchId);

    List<Event> findAllByOrderByEventDateAscStartTimeAsc();

    List<Event> findByStatusOrderByEventDateAscStartTimeAsc(EventStatus status);

    /**
     * Calendario: eventos cuyo rango [eventDate, endDate] se cruza con [from, to].
     * Así un evento de varios días que ya empezó sigue apareciendo mientras dure.
     */
    Page<Event> findByStatusAndEventDateLessThanEqualAndEndDateGreaterThanEqual(
            EventStatus status,
            LocalDate to,
            LocalDate from,
            Pageable pageable);

    // "Próximo" = el siguiente en empezar: alimenta la cuenta regresiva del sitio público.
    Optional<Event> findFirstByStatusAndEventDateGreaterThanEqualOrderByEventDateAscStartTimeAsc(EventStatus status, LocalDate date);

    List<Event> findTop10ByStatusAndEventDateGreaterThanEqualOrderByEventDateAscStartTimeAsc(EventStatus status, LocalDate date);
}
