package com.campuslands.incidents.service.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.campuslands.incidents.dto.indexing.ArticleIndexPayload;
import com.campuslands.incidents.dto.knowledge.KnowledgeArticleRequest;
import com.campuslands.incidents.dto.knowledge.KnowledgeArticleResponse;
import com.campuslands.incidents.entity.KnowledgeArticle;
import com.campuslands.incidents.exception.ResourceNotFoundException;
import com.campuslands.incidents.repository.KnowledgeArticleRepository;

class KnowledgeArticleServiceTest {

    private final KnowledgeArticleRepository repository = mock(KnowledgeArticleRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final KnowledgeArticleService service = new KnowledgeArticleService(repository, events);

    private static KnowledgeArticleRequest request(Boolean verified) {
        return new KnowledgeArticleRequest(" Sin conexión Wi-Fi ", "No conecta", "Ícono con X",
                "Driver desactualizado", "Actualizar el driver", verified);
    }

    /** Ids come from the database, so tests set them by reflection. */
    private static KnowledgeArticle article(long id, boolean active, boolean verified) {
        KnowledgeArticle article = new KnowledgeArticle("Sin conexión Wi-Fi", "No conecta", "Ícono con X",
                "Driver desactualizado", "Actualizar el driver");
        ReflectionTestUtils.setField(article, "id", id);
        article.setActive(active);
        article.setVerified(verified);
        return article;
    }

    /** Payload carried by the single event the service published. */
    private ArticleIndexPayload publishedPayload() {
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        return ((KnowledgeArticleChangedEvent) event.getValue()).payload();
    }

    @Test
    void createIsActiveAndUnverifiedByDefault() {
        when(repository.save(any(KnowledgeArticle.class))).thenAnswer(inv -> {
            KnowledgeArticle saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 7L);
            return saved;
        });

        KnowledgeArticleResponse result = service.create(request(null));

        assertThat(result.title()).isEqualTo("Sin conexión Wi-Fi");
        assertThat(result.active()).isTrue();
        assertThat(result.verified()).isFalse();
        assertThat(result.usableAsSource()).isFalse();
        // Not usable yet, so the indexing flow is told to keep it out of the vector store
        assertThat(publishedPayload().active()).isFalse();
    }

    @Test
    void updateKeepsVerificationWhenItIsOmitted() {
        when(repository.findById(1L)).thenReturn(Optional.of(article(1, true, true)));

        KnowledgeArticleResponse result = service.update(1L, request(null));

        assertThat(result.verified()).isTrue();
        assertThat(result.usableAsSource()).isTrue();
        assertThat(publishedPayload().active()).isTrue();
    }

    @Test
    void updateChangesVerificationWhenItIsSent() {
        when(repository.findById(1L)).thenReturn(Optional.of(article(1, true, true)));

        KnowledgeArticleResponse result = service.update(1L, request(false));

        assertThat(result.verified()).isFalse();
        assertThat(publishedPayload().active()).isFalse();
    }

    @Test
    void deactivateKeepsArticleButRemovesItFromTheSources() {
        when(repository.findById(1L)).thenReturn(Optional.of(article(1, true, true)));

        KnowledgeArticleResponse result = service.deactivate(1L);

        assertThat(result.active()).isFalse();
        assertThat(result.usableAsSource()).isFalse();
        assertThat(publishedPayload().id()).isEqualTo(1L);
        assertThat(publishedPayload().active()).isFalse();
        verify(repository, never()).delete(any());
    }

    @Test
    void activateVerifiedArticleMakesItASourceAgain() {
        when(repository.findById(1L)).thenReturn(Optional.of(article(1, false, true)));

        KnowledgeArticleResponse result = service.activate(1L);

        assertThat(result.usableAsSource()).isTrue();
        assertThat(publishedPayload().active()).isTrue();
    }

    @Test
    void unknownArticleThrowsNotFoundAndPublishesNothing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(events, never()).publishEvent(any(Object.class));
    }
}
