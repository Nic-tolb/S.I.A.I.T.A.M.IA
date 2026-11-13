package com.campuslands.incidents.repository;

import com.campuslands.incidents.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

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
