package com.campuslands.incidents.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Answer generated for a query (at most one per query). Its sources are saved
 * together with it through cascade.
 */
@Entity
@Table(name = "ai_responses")
public class AiResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "query_id", nullable = false, unique = true)
    private AiQuery query;

    @Column(nullable = false, columnDefinition = "text")
    private String answer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "response", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResponseSource> sources = new ArrayList<>();

    protected AiResponse() {
        // required by JPA
    }

    public AiResponse(AiQuery query, String answer) {
        this.query = query;
        this.answer = answer;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Adds a source keeping both sides of the relationship in sync. */
    public void addSource(KnowledgeArticle article, double similarity) {
        sources.add(new ResponseSource(this, article, similarity));
    }

    public Long getId() {
        return id;
    }

    public AiQuery getQuery() {
        return query;
    }

    public String getAnswer() {
        return answer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ResponseSource> getSources() {
        return Collections.unmodifiableList(sources);
    }
}
