package com.campuslands.incidents.service.history;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuslands.incidents.dto.history.RatingRequest;
import com.campuslands.incidents.dto.history.RatingResponse;
import com.campuslands.incidents.entity.AiResponse;
import com.campuslands.incidents.entity.Rating;
import com.campuslands.incidents.exception.ConflictException;
import com.campuslands.incidents.exception.ResourceNotFoundException;
import com.campuslands.incidents.repository.AiResponseRepository;
import com.campuslands.incidents.repository.RatingRepository;

/**
 * Rating of AI answers (RF08, mandatory test 10): usefulness from 1 to 5 with an optional comment,
 * one rating per answer.
 */
@Service
public class RatingService {

    static final String ALREADY_RATED = "Esta respuesta ya fue calificada";

    private final AiResponseRepository responseRepository;
    private final RatingRepository ratingRepository;

    public RatingService(AiResponseRepository responseRepository, RatingRepository ratingRepository) {
        this.responseRepository = responseRepository;
        this.ratingRepository = ratingRepository;
    }

    @Transactional
    public RatingResponse rate(Long responseId, RatingRequest request) {
        AiResponse response = responseRepository.findWithQueryById(responseId)
                .orElseThrow(() -> new ResourceNotFoundException("AiResponse", responseId));
        if (ratingRepository.existsByResponseId(responseId)) {
            throw new ConflictException(ALREADY_RATED);
        }
        try {
            // Flushed here so a concurrent rating hits ratings.response_id UNIQUE inside this method
            Rating saved = ratingRepository.saveAndFlush(new Rating(response, request.score(), normalize(request.comment())));
            return toResponse(saved, response);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(ALREADY_RATED);
        }
    }

    /** Optional comment: trimmed, and stored as null when blank. */
    private static String normalize(String comment) {
        return comment == null || comment.isBlank() ? null : comment.trim();
    }

    static RatingResponse toResponse(Rating rating, AiResponse response) {
        return new RatingResponse(rating.getId(), response.getId(), response.getQuery().getId(),
                rating.getScore(), rating.getComment(), rating.getCreatedAt());
    }
}
