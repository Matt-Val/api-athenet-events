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

    

    Page<Event> findByStatusAndEventDateBetween(
            EventStatus status,
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    List<Event> findByEventDateBetweenOrderByEventDateAsc(LocalDate startDate, LocalDate endDate);
}
