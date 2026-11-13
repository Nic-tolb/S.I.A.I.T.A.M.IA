package com.campuslands.incidents.service.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.campuslands.incidents.dto.indexing.ArticleIndexPayload;
import com.campuslands.incidents.dto.indexing.IndexingResult;
import com.campuslands.incidents.entity.KnowledgeArticle;
import com.campuslands.incidents.repository.KnowledgeArticleRepository;

class KnowledgeReindexServiceTest {

    private final KnowledgeArticleRepository repository = mock(KnowledgeArticleRepository.class);
    private final IndexingService indexingService = mock(IndexingService.class);
    private final KnowledgeReindexService service = new KnowledgeReindexService(repository, indexingService);

    private static KnowledgeArticle article(long id, boolean active, boolean verified) {
        KnowledgeArticle article = new KnowledgeArticle("title " + id, "problem", "symptoms", "cause", "solution");
        ReflectionTestUtils.setField(article, "id", id);
        article.setActive(active);
        article.setVerified(verified);
        return article;
    }

    @Test
    void onlyActiveAndVerifiedArticlesAreSentAsActive() {
        when(repository.findAll(Sort.by("id"))).thenReturn(List.of(
                article(1, true, true),
                article(2, true, false),
                article(3, false, true),
                article(4, false, false)));
        when(indexingService.reindexAll(anyList())).thenReturn(new IndexingResult(1, 3, List.of()));

        IndexingResult result = service.reindexAll();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ArticleIndexPayload>> captor = ArgumentCaptor.forClass(List.class);
        verify(indexingService).reindexAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(ArticleIndexPayload::id, ArticleIndexPayload::active)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, true),
                        org.assertj.core.groups.Tuple.tuple(2L, false),
                        org.assertj.core.groups.Tuple.tuple(3L, false),
                        org.assertj.core.groups.Tuple.tuple(4L, false));
        assertThat(result).isEqualTo(new IndexingResult(1, 3, List.of()));
    }

    @Test
    void payloadCopiesTheArticleContent() {
        ArticleIndexPayload payload = ArticleIndexPayload.from(article(7, true, true));

        assertThat(payload).isEqualTo(
                new ArticleIndexPayload(7, "title 7", "problem", "symptoms", "cause", "solution", true));
    }
}
