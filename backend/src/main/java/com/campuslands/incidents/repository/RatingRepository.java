package com.campuslands.incidents.repository;

import com.campuslands.incidents.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {
}
