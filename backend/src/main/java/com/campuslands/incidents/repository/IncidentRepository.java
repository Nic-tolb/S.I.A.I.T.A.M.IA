package com.campuslands.incidents.repository;

import com.campuslands.incidents.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
}
