package com.campuslands.incidents.repository;

import java.util.List;
import java.util.Optional;

import com.campuslands.incidents.entity.Incident;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Incidents. {@link JpaSpecificationExecutor} lets the incident service combine only the filters
 * the client sent (category, priority, status, dates) without one method per combination.
 */
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    /**
     * Incident list with filters. Category and user are LAZY, and the list shows their names, so
     * they are loaded in the same query: without the entity graph, 150 incidents would cost
     * about 300 extra queries (N+1).
     */
    @Override
    @EntityGraph(attributePaths = {"category", "user"})
    List<Incident> findAll(Specification<Incident> spec, Sort sort);

    /** Single incident with its category and user, for responses that show their names. */
    @EntityGraph(attributePaths = {"category", "user"})
    Optional<Incident> findWithCategoryAndUserById(Long id);

    /**
     * Used before deleting a category: incidents.category_id is ON DELETE RESTRICT,
     * so a category with incidents is reported as 409 instead of failing in the DB.
     */
    boolean existsByCategoryId(Long categoryId);

    /**
     * Used before deleting a user: incidents.user_id is ON DELETE RESTRICT,
     * so a user responsible for incidents is reported as 409 instead of failing in the DB.
     */
    boolean existsByUserId(Long userId);
}
