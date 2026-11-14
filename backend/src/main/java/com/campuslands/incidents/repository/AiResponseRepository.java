package com.campuslands.incidents.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.campuslands.incidents.entity.AiResponse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiResponseRepository extends JpaRepository<AiResponse, Long> {

    /**
     * Responses of several queries at once, with their sources and the cited articles loaded in
     * the same query (history view, RF08). One call per incident instead of one per query.
     */
    @EntityGraph(attributePaths = {"query", "sources", "sources.article"})
    List<AiResponse> findByQueryIdIn(Collection<Long> queryIds);

    /** Response with its query, used when rating (the rating response shows the query id). */
    @EntityGraph(attributePaths = {"query"})
    Optional<AiResponse> findWithQueryById(Long id);
}
