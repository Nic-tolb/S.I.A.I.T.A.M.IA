package com.campuslands.incidents.service.history;

import static com.campuslands.incidents.service.history.HistoryTestData.ANSWER_JSON;
import static com.campuslands.incidents.service.history.HistoryTestData.article;
import static com.campuslands.incidents.service.history.HistoryTestData.incident;
import static com.campuslands.incidents.service.history.HistoryTestData.query;
import static com.campuslands.incidents.service.history.HistoryTestData.rating;
import static com.campuslands.incidents.service.history.HistoryTestData.response;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.campuslands.incidents.dto.assistance.AssistanceSource;
import com.campuslands.incidents.dto.history.QueryHistoryResponse;
import com.campuslands.incidents.entity.AiQuery;
import com.campuslands.incidents.entity.AiResponse;
import com.campuslands.incidents.entity.Incident;
import com.campuslands.incidents.entity.enums.QueryStatus;
import com.campuslands.incidents.exception.ResourceNotFoundException;
import com.campuslands.incidents.repository.AiQueryRepository;
import com.campuslands.incidents.repository.AiResponseRepository;
import com.campuslands.incidents.repository.IncidentRepository;
import com.campuslands.incidents.repository.RatingRepository;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class QueryHistoryServiceTest {

    private final IncidentRepository incidentRepository = mock(IncidentRepository.class);
    private final AiQueryRepository queryRepository = mock(AiQueryRepository.class);
    private final AiResponseRepository responseRepository = mock(AiResponseRepository.class);
    private final RatingRepository ratingRepository = mock(RatingRepository.class);
    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final QueryHistoryService service = new QueryHistoryService(
            incidentRepository, queryRepository, responseRepository, ratingRepository, objectMapper);

    private final Incident incident = incident(1);

    @Test
    void unknownIncidentThrowsNotFound() {
        when(incidentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.findByIncident(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void incidentWithoutQueriesReturnsEmptyHistoryWithoutMoreSelects() {
        when(incidentRepository.existsById(1L)).thenReturn(true);
        when(queryRepository.findByIncidentIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of());

        assertThat(service.findByIncident(1L)).isEmpty();
        verify(responseRepository, never()).findByQueryIdIn(any());
    }

    @Test
    void answeredQueryIncludesAnswerStepsSourcesByMostSimilarAndRating() {
        AiQuery answered = query(10, incident, "2026-11-13T15:00:00Z");
        answered.markAnswered();
        AiResponse response = response(20, answered, ANSWER_JSON);
        response.addSource(article(3, "Sin acceso a internet"), 0.68);
        response.addSource(article(4, "Wi-Fi intermitente"), 0.81);

        when(incidentRepository.existsById(1L)).thenReturn(true);
        when(queryRepository.findByIncidentIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(answered));
        when(responseRepository.findByQueryIdIn(List.of(10L))).thenReturn(List.of(response));
        when(ratingRepository.findByResponseIdIn(List.of(20L))).thenReturn(List.of(rating(30, response, 5, "Útil")));

        QueryHistoryResponse item = service.findByIncident(1L).getFirst();

        assertThat(item.status()).isEqualTo(QueryStatus.ANSWERED);
        assertThat(item.responseId()).isEqualTo(20L);
        assertThat(item.answer().solution()).isEqualTo("Reiniciar el router");
        assertThat(item.answer().steps()).containsExactly("Apagar", "Encender");
        assertThat(item.sources()).extracting(AssistanceSource::articleId).containsExactly(4L, 3L);
        assertThat(item.rating().score()).isEqualTo(5);
        assertThat(item.rating().queryId()).isEqualTo(10L);
    }

    @Test
    void noResultsQueryHasNoAnswerSourcesOrRating() {
        AiQuery noResults = query(11, incident, "2026-11-13T16:00:00Z");
        noResults.markNoResults();

        when(incidentRepository.existsById(1L)).thenReturn(true);
        when(queryRepository.findByIncidentIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(noResults));
        when(responseRepository.findByQueryIdIn(List.of(11L))).thenReturn(List.of());

        QueryHistoryResponse item = service.findByIncident(1L).getFirst();

        assertThat(item.status()).isEqualTo(QueryStatus.NO_RESULTS);
        assertThat(item.responseId()).isNull();
        assertThat(item.answer()).isNull();
        assertThat(item.sources()).isEmpty();
        assertThat(item.rating()).isNull();
        verify(ratingRepository, never()).findByResponseIdIn(any());
    }

    @Test
    void corruptAnswerIsShownEmptyWithoutHidingTheQuery() {
        AiQuery answered = query(12, incident, "2026-11-13T17:00:00Z");
        answered.markAnswered();
        AiResponse corrupt = response(21, answered, "not json");

        when(incidentRepository.existsById(1L)).thenReturn(true);
        when(queryRepository.findByIncidentIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(answered));
        when(responseRepository.findByQueryIdIn(List.of(12L))).thenReturn(List.of(corrupt));
        when(ratingRepository.findByResponseIdIn(List.of(21L))).thenReturn(List.of());

        QueryHistoryResponse item = service.findByIncident(1L).getFirst();

        assertThat(item.responseId()).isEqualTo(21L);
        assertThat(item.answer()).isNull();
    }
}
