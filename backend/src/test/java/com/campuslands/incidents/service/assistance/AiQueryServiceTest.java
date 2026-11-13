package com.campuslands.incidents.service.assistance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.campuslands.incidents.client.N8nClient;
import com.campuslands.incidents.config.N8nProperties;
import com.campuslands.incidents.config.RagProperties;
import com.campuslands.incidents.dto.assistance.AssistanceAnswer;
import com.campuslands.incidents.dto.assistance.AssistanceResponse;
import com.campuslands.incidents.dto.assistance.AssistanceSource;
import com.campuslands.incidents.dto.rag.RagRequest;
import com.campuslands.incidents.dto.rag.RagResponse;
import com.campuslands.incidents.entity.AiQuery;
import com.campuslands.incidents.entity.AiResponse;
import com.campuslands.incidents.entity.Category;
import com.campuslands.incidents.entity.Incident;
import com.campuslands.incidents.entity.KnowledgeArticle;
import com.campuslands.incidents.entity.User;
import com.campuslands.incidents.entity.enums.IncidentPriority;
import com.campuslands.incidents.entity.enums.QueryStatus;
import com.campuslands.incidents.exception.AiServiceException;
import com.campuslands.incidents.exception.ResourceNotFoundException;
import com.campuslands.incidents.repository.AiQueryRepository;
import com.campuslands.incidents.repository.AiResponseRepository;
import com.campuslands.incidents.repository.IncidentRepository;
import com.campuslands.incidents.repository.KnowledgeArticleRepository;

import tools.jackson.databind.json.JsonMapper;

class AiQueryServiceTest {

    private static final String RAG_PATH = "/webhook/rag-assistant";
    private static final long INCIDENT_ID = 7L;
    private static final long QUERY_ID = 42L;

    private final IncidentRepository incidentRepository = mock(IncidentRepository.class);
    private final AiQueryRepository aiQueryRepository = mock(AiQueryRepository.class);
    private final AiResponseRepository aiResponseRepository = mock(AiResponseRepository.class);
    private final KnowledgeArticleRepository articleRepository = mock(KnowledgeArticleRepository.class);
    private final N8nClient n8nClient = mock(N8nClient.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

    private final Incident incident = incident();
    private AiQueryService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new AiQueryService(incidentRepository, aiQueryRepository, aiResponseRepository, articleRepository,
                n8nClient,
                new N8nProperties("http://n8n:5678", "secret", "/webhook/index-articles", RAG_PATH,
                        Duration.ofSeconds(5), Duration.ofSeconds(60)),
                new RagProperties(0.60, 5),
                transactionTemplate,
                new JsonMapper());

        when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));
        // save() assigns the id and returns the same object, like JPA does
        when(aiQueryRepository.save(any(AiQuery.class))).thenAnswer(invocation -> {
            AiQuery query = invocation.getArgument(0);
            if (query.getId() == null) {
                ReflectionTestUtils.setField(query, "id", QUERY_ID);
            }
            return query;
        });
        when(aiQueryRepository.findById(QUERY_ID)).thenAnswer(invocation -> Optional.of(savedQuery()));
        // The template just runs the callback
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                invocation.<TransactionCallback<Object>>getArgument(0).doInTransaction(null));
    }

    @Test
    void answeredQueryIsStoredWithValidatedSourcesOnly() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article(1, true, true)));
        when(articleRepository.findById(2L)).thenReturn(Optional.of(article(2, false, true)));
        when(articleRepository.findById(3L)).thenReturn(Optional.empty());
        AssistanceAnswer answer = new AssistanceAnswer("Renovar la IP", List.of("Abrir cmd", "ipconfig /renew"));
        when(n8nClient.post(eq(RAG_PATH), any(), eq(RagResponse.class))).thenReturn(new RagResponse("ANSWERED", answer,
                List.of(new AssistanceSource(1L, "Sin internet", 0.64),
                        new AssistanceSource(2L, "Inactivo", 0.61),
                        new AssistanceSource(3L, "Borrado", 0.60)), null));

        AssistanceResponse result = service.assist(INCIDENT_ID, "  No hay internet en el piso 3  ");

        assertThat(result.status()).isEqualTo(QueryStatus.ANSWERED);
        assertThat(result.queryId()).isEqualTo(QUERY_ID);
        assertThat(result.incidentId()).isEqualTo(INCIDENT_ID);
        assertThat(result.answer()).isEqualTo(answer);
        assertThat(result.sources()).containsExactly(new AssistanceSource(1L, "title 1", 0.64));
        assertThat(result.errorMessage()).isNull();

        ArgumentCaptor<RagRequest> request = ArgumentCaptor.forClass(RagRequest.class);
        verify(n8nClient).post(eq(RAG_PATH), request.capture(), eq(RagResponse.class));
        assertThat(request.getValue()).isEqualTo(new RagRequest(QUERY_ID, "No hay internet en el piso 3", 0.60, 5));

        ArgumentCaptor<AiResponse> stored = ArgumentCaptor.forClass(AiResponse.class);
        verify(aiResponseRepository).save(stored.capture());
        assertThat(stored.getValue().getAnswer()).contains("\"solution\":\"Renovar la IP\"").contains("ipconfig /renew");
        assertThat(stored.getValue().getSources()).hasSize(1);
        assertThat(stored.getValue().getSources().get(0).getArticle().getId()).isEqualTo(1L);
    }

    @Test
    void answerBuiltOnlyOnUnusableSourcesEndsAsNoResults() {
        when(articleRepository.findById(2L)).thenReturn(Optional.of(article(2, true, false)));
        when(n8nClient.post(eq(RAG_PATH), any(), eq(RagResponse.class))).thenReturn(new RagResponse("ANSWERED",
                new AssistanceAnswer("x", List.of("y")), List.of(new AssistanceSource(2L, "Sin verificar", 0.7)), null));

        AssistanceResponse result = service.assist(INCIDENT_ID, "Pregunta");

        assertThat(result.status()).isEqualTo(QueryStatus.NO_RESULTS);
        assertThat(result.answer()).isNull();
        assertThat(result.sources()).isEmpty();
        verify(aiResponseRepository, never()).save(any());
    }

    @Test
    void noResultsFromN8nIsRecorded() {
        when(n8nClient.post(eq(RAG_PATH), any(), eq(RagResponse.class)))
                .thenReturn(new RagResponse("NO_RESULTS", null, List.of(), null));

        AssistanceResponse result = service.assist(INCIDENT_ID, "La impresora 3D no calibra");

        assertThat(result.status()).isEqualTo(QueryStatus.NO_RESULTS);
        assertThat(savedQuery().getStatus()).isEqualTo(QueryStatus.NO_RESULTS);
        verify(aiResponseRepository, never()).save(any());
    }

    @Test
    void aiFailureEndsAsErrorWithoutThrowing() {
        when(n8nClient.post(eq(RAG_PATH), any(), eq(RagResponse.class)))
                .thenThrow(new AiServiceException("AI service is unavailable or timed out"));

        AssistanceResponse result = service.assist(INCIDENT_ID, "Sin internet");

        assertThat(result.status()).isEqualTo(QueryStatus.ERROR);
        assertThat(result.errorMessage()).isEqualTo("AI service is unavailable or timed out");
        assertThat(savedQuery().getStatus()).isEqualTo(QueryStatus.ERROR);
        assertThat(savedQuery().getErrorMessage()).isEqualTo("AI service is unavailable or timed out");
    }

    @Test
    void errorStatusFromN8nIsRecordedWithItsMessage() {
        when(n8nClient.post(eq(RAG_PATH), any(), eq(RagResponse.class)))
                .thenReturn(new RagResponse("ERROR", null, List.of(), "Model returned invalid JSON"));

        AssistanceResponse result = service.assist(INCIDENT_ID, "Sin internet");

        assertThat(result.status()).isEqualTo(QueryStatus.ERROR);
        assertThat(result.errorMessage()).isEqualTo("Model returned invalid JSON");
    }

    @Test
    void unknownIncidentIsRejectedBeforeAnythingIsSaved() {
        assertThatThrownBy(() -> service.assist(99L, "Sin internet"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(aiQueryRepository, never()).save(any());
        verify(n8nClient, never()).post(any(), any(), any());
    }

    private AiQuery savedQuery() {
        ArgumentCaptor<AiQuery> captor = ArgumentCaptor.forClass(AiQuery.class);
        verify(aiQueryRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private static Incident incident() {
        Incident incident = new Incident("Sin internet", "No hay internet", new Category("Conectividad", null),
                new User("Laura", "laura@empresa.com", "Ventas"), IncidentPriority.P2);
        ReflectionTestUtils.setField(incident, "id", INCIDENT_ID);
        return incident;
    }

    private static KnowledgeArticle article(long id, boolean active, boolean verified) {
        KnowledgeArticle article = new KnowledgeArticle("title " + id, "p", "s", "c", "sol");
        ReflectionTestUtils.setField(article, "id", id);
        article.setActive(active);
        article.setVerified(verified);
        return article;
    }
}
