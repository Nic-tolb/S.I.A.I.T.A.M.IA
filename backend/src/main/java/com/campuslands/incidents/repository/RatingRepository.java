package com.campuslands.incidents.repository;

import java.util.Collection;
import java.util.List;

import com.campuslands.incidents.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    /** Ratings of several responses at once (history view, RF08). */
    List<Rating> findByResponseIdIn(Collection<Long> responseIds);

    /**
     * One rating per response: ratings.response_id is UNIQUE, so a second rating is reported
     * as 409 instead of failing in the database.
     */
    boolean existsByResponseId(Long responseId);
}
