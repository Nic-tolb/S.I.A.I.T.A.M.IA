package com.campuslands.incidents.entity;

import com.campuslands.incidents.entity.enums.QueryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One AI assistance request for an incident. Created as PENDING and then moved
 * to ANSWERED, NO_RESULTS or ERROR.
 */
@Entity
@Table(name = "ai_queries")
public class AiQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueryStatus status = QueryStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiQuery() {
        // required by JPA
    }

    public AiQuery(Incident incident, String question) {
        this.incident = incident;
        this.question = question;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void markAnswered() {
        this.status = QueryStatus.ANSWERED;
        this.errorMessage = null;
    }

    public void markNoResults() {
        this.status = QueryStatus.NO_RESULTS;
        this.errorMessage = null;
    }

    public void markError(String errorMessage) {
        this.status = QueryStatus.ERROR;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public Incident getIncident() {
        return incident;
    }

    public String getQuestion() {
        return question;
    }

    public QueryStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
