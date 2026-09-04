package com.athenet.events.service;

import com.athenet.events.model.Event;
import com.athenet.events.model.EventStatus;
import com.athenet.events.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

@Service
public class EventService {
    
    private final EventRepository eventRepository;

    // Inyección de dependencias a través del constructor
    public EventService(EventRepository eventRepository) { 
        this.eventRepository = eventRepository;
    }

    // ==========================================
    // VISTA PÚBLICA (Acceso libre)
    // ==========================================

    public List<Event> getAllEvents() { 
        return eventRepository.findAll();
    }

    public Page<Event> getUpcomingEvents(
        int daysAhead,
        EventStatus status,
        Pageable pageable) {

            LocalDate today = LocalDate.now();
            LocalDate futureDate = today.plusDays(daysAhead);

            return eventRepository.findByStatusAndEventDateBetween(
                status,
                today,
                futureDate,
                pageable);
        }

    public Event getEventByInternalId(String internalId) { 
        return eventRepository.findByInternalId(internalId)
            .orElseThrow( () -> new RuntimeException ("Error: Evento no encontrado con ID: " + internalId));
            
    }

    // ==========================================
    // VISTA ADMIN ORG (GESTION INTERNA)
    // ==========================================

    // Método para crear un nuevo evento
    public Event createEvent(Event event) { 
        return eventRepository.save(event);
    }

    // Método para actualizar un evento ya existente.
    public Event updateEvent(Long id, Event updatedEvent) { 
        // Busca el evento y si existe se actualiza (ID)
        return eventRepository.findById(id).map(existingEvent -> { 
            // Datos generales.
            existingEvent.setInternalId(updatedEvent.getInternalId());
            existingEvent.setTitle(updatedEvent.getTitle());
            existingEvent.setDescription(updatedEvent.getDescription());
            existingEvent.setDescription_opt(updatedEvent.getDescription_opt());
            
            // imagenes, tanto el cover como el arraylist de fotos
            existingEvent.setCoverImage(updatedEvent.getCoverImage());
            existingEvent.getPhotos().clear();
            if (updatedEvent.getPhotos() != null) { 
                existingEvent.getPhotos().addAll(updatedEvent.getPhotos());
            }

            // ENUMS y fechas
            existingEvent.setType(updatedEvent.getType());
            existingEvent.setCategory(updatedEvent.getCategory());
            existingEvent.setEventDate(updatedEvent.getEventDate());
            existingEvent.setStatus(updatedEvent.getStatus());


            // Flags y Teams
            existingEvent.setOfficial(updatedEvent.isOfficial());
            existingEvent.setOrganizationId(updatedEvent.getOrganizationId());
            existingEvent.setTeamOneId(updatedEvent.getTeamOneId());
            existingEvent.setTeamTwoId(updatedEvent.getTeamTwoId());

            return eventRepository.save(existingEvent);
        }).orElseThrow( () -> new RuntimeException("Error: Evento no encontrado con ID " + id));
    }

    public void deleteEvent(Long id) { 
        if (!eventRepository.existsById(id)) { 
            throw new RuntimeException("Error: El evento no está registrado");
        }
        eventRepository.deleteById(id);
    }

}
