package com.campuslands.incidents.service.incident;

import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuslands.incidents.dto.incident.IncidentFilter;
import com.campuslands.incidents.dto.incident.IncidentRequest;
import com.campuslands.incidents.dto.incident.IncidentResponse;
import com.campuslands.incidents.dto.incident.IncidentStatusUpdateRequest;
import com.campuslands.incidents.entity.Category;
import com.campuslands.incidents.entity.Incident;
import com.campuslands.incidents.entity.User;
import com.campuslands.incidents.entity.enums.IncidentStatus;
import com.campuslands.incidents.exception.ResourceNotFoundException;
import com.campuslands.incidents.repository.CategoryRepository;
import com.campuslands.incidents.repository.IncidentRepository;
import com.campuslands.incidents.repository.UserRepository;

/**
 * Incident management (RF01): register, list with filters and change the status.
 * Incidents are not edited or deleted: they are the history the analytics are computed from.
 */
@Service
@Transactional(readOnly = true)
public class IncidentService {

    /** Newest first; id breaks ties between incidents created in the same instant. */
    static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final IncidentRepository incidentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ZoneId zone;

    /**
     * @param timeZone same TZ variable PostgreSQL uses (.env), so date filters and the analytics
     *                 SQL agree on what a "day" is
     */
    public IncidentService(IncidentRepository incidentRepository,
                           CategoryRepository categoryRepository,
                           UserRepository userRepository,
                           @Value("${TZ:America/Bogota}") String timeZone) {
        this.incidentRepository = incidentRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.zone = ZoneId.of(timeZone);
    }

    public List<IncidentResponse> findAll(IncidentFilter filter) {
        return incidentRepository.findAll(IncidentSpecifications.fromFilter(filter, zone), NEWEST_FIRST).stream()
                .map(IncidentService::toResponse)
                .toList();
    }

    public IncidentResponse findById(Long id) {
        return toResponse(getIncident(id));
    }

    @Transactional
    public IncidentResponse create(IncidentRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));

        Incident incident = incidentRepository.save(new Incident(
                request.title().trim(), request.description().trim(), category, user, request.priority()));

        // Saved first, so createdAt is set before a RESOLVED/CLOSED status sets resolvedAt:
        // the other way round resolved_at would be earlier than created_at and break the DB CHECK.
        IncidentStatus status = request.statusOrDefault();
        if (status != IncidentStatus.OPEN) {
            incident.changeStatus(status);
        }
        return toResponse(incident);
    }

    /** RESOLVED/CLOSED set resolvedAt and OPEN/IN_PROGRESS clear it (Incident#changeStatus). */
    @Transactional
    public IncidentResponse changeStatus(Long id, IncidentStatusUpdateRequest request) {
        Incident incident = getIncident(id);
        incident.changeStatus(request.status());
        return toResponse(incident);
    }

    private Incident getIncident(Long id) {
        return incidentRepository.findWithCategoryAndUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", id));
    }

    static IncidentResponse toResponse(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getPriority(),
                incident.getStatus(),
                incident.getCategory().getId(),
                incident.getCategory().getName(),
                incident.getUser().getId(),
                incident.getUser().getName(),
                incident.getCreatedAt(),
                incident.getResolvedAt());
    }
}
