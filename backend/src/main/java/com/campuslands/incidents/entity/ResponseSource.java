package com.campuslands.incidents.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Knowledge article used as a source of a response, with its similarity score (0..1).
 * Created through {@link AiResponse#addSource(KnowledgeArticle, double)}.
 */
@Entity
@Table(name = "response_sources",
        uniqueConstraints = @UniqueConstraint(columnNames = {"response_id", "article_id"}))
public class ResponseSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "response_id", nullable = false)
    private AiResponse response;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private KnowledgeArticle article;

    @Column(nullable = false)
    private Double similarity;

    protected ResponseSource() {
        // required by JPA
    }

    ResponseSource(AiResponse response, KnowledgeArticle article, double similarity) {
        this.response = response;
        this.article = article;
        this.similarity = similarity;
    }

    public Long getId() {
        return id;
    }

    public AiResponse getResponse() {
        return response;
    }

    public KnowledgeArticle getArticle() {
        return article;
    }

    public Double getSimilarity() {
        return similarity;
    }
}
