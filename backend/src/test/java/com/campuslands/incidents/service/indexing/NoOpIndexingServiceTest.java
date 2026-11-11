package com.campuslands.incidents.service.indexing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.campuslands.incidents.dto.indexing.ArticleIndexPayload;
import com.campuslands.incidents.dto.indexing.IndexingResult;

class NoOpIndexingServiceTest {

    private final IndexingService service = new NoOpIndexingService();

    private final ArticleIndexPayload article =
            new ArticleIndexPayload(1L, "title", "problem", "symptoms", "cause", "solution", true);

    @Test
    void syncArticleReturnsEmptySuccessfulResult() {
        IndexingResult result = service.syncArticle(article);

        assertThat(result.indexed()).isZero();
        assertThat(result.deleted()).isZero();
        assertThat(result.successful()).isTrue();
    }

    @Test
    void removeAndReindexDoNotFail() {
        assertThat(service.removeArticle(1L).successful()).isTrue();
        assertThat(service.reindexAll(List.of(article)).successful()).isTrue();
    }

    @Test
    void indexingResultNormalizesNullErrors() {
        assertThat(new IndexingResult(1, 0, null).errors()).isEmpty();
        assertThat(IndexingResult.failed("n8n down").successful()).isFalse();
    }
}
