package com.campuslands.incidents.service.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.campuslands.incidents.client.N8nClient;
import com.campuslands.incidents.config.N8nProperties;
import com.campuslands.incidents.dto.indexing.ArticleIndexPayload;
import com.campuslands.incidents.dto.indexing.IndexArticlesRequest;
import com.campuslands.incidents.dto.indexing.IndexingResult;
import com.campuslands.incidents.exception.AiServiceException;

class N8nIndexingServiceTest {

    private static final String PATH = "/webhook/index-articles";

    private final N8nClient client = mock(N8nClient.class);
    private final IndexingService service = new N8nIndexingService(client,
            new N8nProperties("http://n8n:5678", "secret", PATH, "/webhook/rag-assistant",
                    Duration.ofSeconds(5), Duration.ofSeconds(60)));

    private static ArticleIndexPayload article(long id, boolean active) {
        return new ArticleIndexPayload(id, "title " + id, "problem", "symptoms", "cause", "solution", active);
    }

    private ArgumentCaptor<IndexArticlesRequest> captureRequests(int times) {
        ArgumentCaptor<IndexArticlesRequest> captor = ArgumentCaptor.forClass(IndexArticlesRequest.class);
        verify(client, times(times)).post(eq(PATH), captor.capture(), eq(IndexingResult.class));
        return captor;
    }

    @Test
    void syncArticleSendsTheArticleAndReturnsTheFlowResult() {
        when(client.post(eq(PATH), any(), eq(IndexingResult.class))).thenReturn(new IndexingResult(1, 0, List.of()));

        IndexingResult result = service.syncArticle(article(7, true));

        assertThat(result).isEqualTo(new IndexingResult(1, 0, List.of()));
        assertThat(captureRequests(1).getValue().articles()).containsExactly(article(7, true));
    }

    @Test
    void removeArticleSendsItAsInactive() {
        when(client.post(eq(PATH), any(), eq(IndexingResult.class))).thenReturn(new IndexingResult(0, 1, List.of()));

        IndexingResult result = service.removeArticle(9);

        assertThat(result.deleted()).isEqualTo(1);
        ArticleIndexPayload sent = captureRequests(1).getValue().articles().get(0);
        assertThat(sent.id()).isEqualTo(9);
        assertThat(sent.active()).isFalse();
    }

    @Test
    void n8nFailureIsReturnedAsErrorInsteadOfThrown() {
        when(client.post(eq(PATH), any(), eq(IndexingResult.class)))
                .thenThrow(new AiServiceException("AI service is unavailable or timed out"));

        IndexingResult result = service.syncArticle(article(3, true));

        assertThat(result.successful()).isFalse();
        assertThat(result.errors()).singleElement().asString()
                .contains("[3]")
                .contains("unavailable");
    }

    @Test
    void emptyResponseIsReportedAsError() {
        when(client.post(eq(PATH), any(), eq(IndexingResult.class))).thenReturn(null);

        assertThat(service.syncArticle(article(4, true)).successful()).isFalse();
    }

    @Test
    void reindexAllSplitsIntoBatchesAndAddsUpTheResults() {
        int total = N8nIndexingService.BATCH_SIZE * 2 + 5;
        List<ArticleIndexPayload> articles = LongStream.rangeClosed(1, total).mapToObj(id -> article(id, id % 5 != 0)).toList();
        when(client.post(eq(PATH), any(), eq(IndexingResult.class))).thenAnswer(invocation -> {
            List<ArticleIndexPayload> batch = invocation.<IndexArticlesRequest>getArgument(1).articles();
            int active = (int) batch.stream().filter(ArticleIndexPayload::active).count();
            return new IndexingResult(active, batch.size() - active, List.of());
        });

        IndexingResult result = service.reindexAll(articles);

        List<IndexArticlesRequest> requests = captureRequests(3).getAllValues();
        assertThat(requests).extracting(r -> r.articles().size())
                .containsExactly(N8nIndexingService.BATCH_SIZE, N8nIndexingService.BATCH_SIZE, 5);
        assertThat(result.indexed() + result.deleted()).isEqualTo(total);
        assertThat(result.deleted()).isEqualTo(total / 5);
        assertThat(result.successful()).isTrue();
    }

    @Test
    void reindexAllContinuesWhenOneBatchFails() {
        List<ArticleIndexPayload> articles = LongStream.rangeClosed(1, N8nIndexingService.BATCH_SIZE + 1)
                .mapToObj(id -> article(id, true)).toList();
        when(client.post(eq(PATH), any(), eq(IndexingResult.class)))
                .thenThrow(new AiServiceException("AI service responded with error 502"))
                .thenReturn(new IndexingResult(1, 0, List.of()));

        IndexingResult result = service.reindexAll(articles);

        assertThat(result.indexed()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
    }

    @Test
    void reindexAllWithNoArticlesDoesNotCallN8n() {
        assertThat(service.reindexAll(List.of())).isEqualTo(IndexingResult.empty());
        verify(client, never()).post(any(), any(), any());
    }

    @Test
    void indexingResultNormalizesNullErrors() {
        assertThat(new IndexingResult(1, 0, null).errors()).isEmpty();
        assertThat(IndexingResult.failed("n8n down").successful()).isFalse();
    }
}
