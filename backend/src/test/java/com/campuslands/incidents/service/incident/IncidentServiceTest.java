package com.campuslands.incidents.service.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.campuslands.incidents.dto.incident.IncidentFilter;
import com.campuslands.incidents.dto.incident.IncidentRequest;
import com.campuslands.incidents.dto.incident.IncidentResponse;
import com.campuslands.incidents.dto.incident.IncidentStatusUpdateRequest;
import com.campuslands.incidents.entity.Category;
import com.campuslands.incidents.entity.Incident;
import com.campuslands.incidents.entity.User;
import com.campuslands.incidents.entity.enums.IncidentPriority;
import com.campuslands.incidents.entity.enums.IncidentStatus;
import com.campuslands.incidents.exception.ResourceNotFoundException;
import com.campuslands.incidents.repository.CategoryRepository;
import com.campuslands.incidents.repository.IncidentRepository;
import com.campuslands.incidents.repository.UserRepository;

class IncidentServiceTest {

    private final IncidentRepository incidentRepository = mock(IncidentRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final IncidentService service =
            new IncidentService(incidentRepository, categoryRepository, userRepository, "America/Bogota");

    private final Category connectivity = withId(new Category("Conectividad", null), 2L);
    private final User laura = withId(new User("Laura Gómez", "laura@empresa.com", "Finanzas"), 5L);

    /** Ids and creation dates come from the database, so tests set them by reflection. */
    private static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static Incident incident(long id, Category category, User user) {
        Incident incident = withId(new Incident("Sin internet", "No hay conexión en el piso 3",
                category, user, IncidentPriority.P2), id);
        ReflectionTestUtils.setField(incident, "createdAt", Instant.parse("2026-10-03T14:15:00Z"));
        return incident;
    }

    private static IncidentRequest request(IncidentStatus status) {
        return new IncidentRequest("  Sin internet ", " No hay conexión en el piso 3 ", 2L,
                IncidentPriority.P2, status, 5L);
    }

    @BeforeEach
    void knownCategoryAndUser() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(connectivity));
        when(userRepository.findById(5L)).thenReturn(Optional.of(laura));
        // Simulates the database: save() assigns the id and @PrePersist sets createdAt
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> {
            Incident saved = withId(inv.getArgument(0), 10L);
            ReflectionTestUtils.setField(saved, "createdAt", Instant.now());
            return saved;
        });
    }

    @Test
    void findAllSortsNewestFirst() {
        when(incidentRepository.findAll(any(Specification.class), eq(IncidentService.NEWEST_FIRST)))
                .thenReturn(List.of(incident(1, connectivity, laura)));

        List<IncidentResponse> result = service.findAll(new IncidentFilter(null, null, null, null, null));

        assertThat(result).hasSize(1);
        verify(incidentRepository).findAll(any(Specification.class), eq(IncidentService.NEWEST_FIRST));
    }

    @Test
    void responseIncludesCategoryAndUserNames() {
        when(incidentRepository.findWithCategoryAndUserById(1L)).thenReturn(Optional.of(incident(1, connectivity, laura)));

        IncidentResponse result = service.findById(1L);

        assertThat(result.categoryId()).isEqualTo(2L);
        assertThat(result.categoryName()).isEqualTo("Conectividad");
        assertThat(result.userId()).isEqualTo(5L);
        assertThat(result.userName()).isEqualTo("Laura Gómez");
    }

    @Test
    void createIsOpenByDefaultAndTrimsTexts() {
        IncidentResponse result = service.create(request(null));

        assertThat(result.status()).isEqualTo(IncidentStatus.OPEN);
        assertThat(result.resolvedAt()).isNull();
        assertThat(result.title()).isEqualTo("Sin internet");
        assertThat(result.description()).isEqualTo("No hay conexión en el piso 3");
    }

    @Test
    void createAsResolvedSetsResolvedAtNotBeforeCreatedAt() {
        IncidentResponse result = service.create(request(IncidentStatus.RESOLVED));

        assertThat(result.status()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(result.resolvedAt()).isNotNull().isAfterOrEqualTo(result.createdAt());
    }

    @Test
    void createRejectsUnknownCategory() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(null))).isInstanceOf(ResourceNotFoundException.class);
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownUser() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(null))).isInstanceOf(ResourceNotFoundException.class);
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void resolvingSetsResolvedAtAndReopeningClearsIt() {
        when(incidentRepository.findWithCategoryAndUserById(1L)).thenReturn(Optional.of(incident(1, connectivity, laura)));

        IncidentResponse resolved = service.changeStatus(1L, new IncidentStatusUpdateRequest(IncidentStatus.RESOLVED));
        IncidentResponse reopened = service.changeStatus(1L, new IncidentStatusUpdateRequest(IncidentStatus.OPEN));

        assertThat(resolved.resolvedAt()).isNotNull();
        assertThat(reopened.status()).isEqualTo(IncidentStatus.OPEN);
        assertThat(reopened.resolvedAt()).isNull();
    }

    @Test
    void changeStatusThrowsNotFoundForUnknownIncident() {
        when(incidentRepository.findWithCategoryAndUserById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(99L, new IncidentStatusUpdateRequest(IncidentStatus.CLOSED)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
