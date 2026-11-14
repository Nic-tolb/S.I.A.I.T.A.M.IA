package com.campuslands.incidents.service.history;

import static com.campuslands.incidents.service.history.HistoryTestData.ANSWER_JSON;
import static com.campuslands.incidents.service.history.HistoryTestData.incident;
import static com.campuslands.incidents.service.history.HistoryTestData.query;
import static com.campuslands.incidents.service.history.HistoryTestData.response;
import static com.campuslands.incidents.service.history.HistoryTestData.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import com.campuslands.incidents.dto.history.RatingRequest;
import com.campuslands.incidents.dto.history.RatingResponse;
import com.campuslands.incidents.entity.AiResponse;
import com.campuslands.incidents.entity.Rating;
import com.campuslands.incidents.exception.ConflictException;
import com.campuslands.incidents.exception.ResourceNotFoundException;
import com.campuslands.incidents.repository.AiResponseRepository;
import com.campuslands.incidents.repository.RatingRepository;

class RatingServiceTest {

    private final AiResponseRepository responseRepository = mock(AiResponseRepository.class);
    private final RatingRepository ratingRepository = mock(RatingRepository.class);
    private final RatingService service = new RatingService(responseRepository, ratingRepository);

    private final AiResponse response = response(20, query(10, incident(1), "2026-11-13T15:00:00Z"), ANSWER_JSON);

    @BeforeEach
    void knownResponse() {
        when(responseRepository.findWithQueryById(20L)).thenReturn(Optional.of(response));
    }

    @Test
    void ratingIsStoredWithTrimmedComment() {
        when(ratingRepository.saveAndFlush(any(Rating.class))).thenAnswer(inv -> withId(inv.getArgument(0), 30L));

        RatingResponse result = service.rate(20L, new RatingRequest(4, "  Me sirvió  "));

        ArgumentCaptor<Rating> saved = ArgumentCaptor.forClass(Rating.class);
        verify(ratingRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getScore()).isEqualTo(4);
        assertThat(saved.getValue().getComment()).isEqualTo("Me sirvió");
        assertThat(result.id()).isEqualTo(30L);
        assertThat(result.responseId()).isEqualTo(20L);
        assertThat(result.queryId()).isEqualTo(10L);
    }

    @Test
    void blankCommentIsStoredAsNull() {
        when(ratingRepository.saveAndFlush(any(Rating.class))).thenAnswer(inv -> withId(inv.getArgument(0), 31L));

        RatingResponse result = service.rate(20L, new RatingRequest(3, "   "));

        assertThat(result.comment()).isNull();
    }

    @Test
    void unknownResponseThrowsNotFound() {
        when(responseRepository.findWithQueryById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rate(99L, new RatingRequest(5, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void secondRatingIsRejectedWithConflict() {
        when(ratingRepository.existsByResponseId(20L)).thenReturn(true);

        assertThatThrownBy(() -> service.rate(20L, new RatingRequest(5, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage(RatingService.ALREADY_RATED);
        verify(ratingRepository, never()).saveAndFlush(any());
    }

    @Test
    void concurrentRatingHittingTheUniqueConstraintIsAConflict() {
        when(ratingRepository.saveAndFlush(any(Rating.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key ratings_response_id_key"));

        assertThatThrownBy(() -> service.rate(20L, new RatingRequest(5, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage(RatingService.ALREADY_RATED);
    }
}
