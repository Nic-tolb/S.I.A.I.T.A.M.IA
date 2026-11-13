package com.campuslands.incidents.service.knowledge;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.campuslands.incidents.dto.indexing.ArticleIndexPayload;
import com.campuslands.incidents.dto.indexing.IndexingResult;
import com.campuslands.incidents.service.indexing.IndexingService;

class KnowledgeIndexingListenerTest {

    private final IndexingService indexingService = mock(IndexingService.class);
    private final KnowledgeIndexingListener listener = new KnowledgeIndexingListener(indexingService);

    private final ArticleIndexPayload payload =
            new ArticleIndexPayload(3L, "title", "problem", "symptoms", "cause", "solution", false);
    private final KnowledgeArticleChangedEvent event = new KnowledgeArticleChangedEvent(payload);

    @Test
    void syncsTheArticleFromTheEvent() {
        when(indexingService.syncArticle(payload)).thenReturn(IndexingResult.empty());

        listener.onArticleChanged(event);

        verify(indexingService).syncArticle(payload);
    }

    @Test
    void indexingErrorsDoNotFailTheRequest() {
        when(indexingService.syncArticle(payload)).thenReturn(IndexingResult.failed("n8n is down"));

        assertThatCode(() -> listener.onArticleChanged(event)).doesNotThrowAnyException();
    }

    @Test
    void unexpectedExceptionsDoNotFailTheRequest() {
        when(indexingService.syncArticle(payload)).thenThrow(new IllegalStateException("boom"));

        assertThatCode(() -> listener.onArticleChanged(event)).doesNotThrowAnyException();
    }
}
