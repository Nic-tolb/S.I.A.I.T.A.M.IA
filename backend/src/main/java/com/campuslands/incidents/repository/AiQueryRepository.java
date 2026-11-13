package com.campuslands.incidents.repository;

import java.util.List;

import com.campuslands.incidents.entity.AiQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiQueryRepository extends JpaRepository<AiQuery, Long> {

    /** History of an incident (RF08): every AI query, newest first. */
    List<AiQuery> findByIncidentIdOrderByCreatedAtDescIdDesc(Long incidentId);
}
