package com.campuslands.incidents.service.history;

import java.time.Instant;

import org.springframework.test.util.ReflectionTestUtils;

import com.campuslands.incidents.entity.AiQuery;
import com.campuslands.incidents.entity.AiResponse;
import com.campuslands.incidents.entity.Category;
import com.campuslands.incidents.entity.Incident;
import com.campuslands.incidents.entity.KnowledgeArticle;
import com.campuslands.incidents.entity.Rating;
import com.campuslands.incidents.entity.User;
import com.campuslands.incidents.entity.enums.IncidentPriority;

/** Entities for the history tests. Ids and dates come from the database, so they are set by reflection. */
final class HistoryTestData {

    static final String ANSWER_JSON = "{\"solution\":\"Reiniciar el router\",\"steps\":[\"Apagar\",\"Encender\"]}";

    private HistoryTestData() {
    }

    static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    static Incident incident(long id) {
        return withId(new Incident("Sin internet", "No hay conexión", new Category("Conectividad", null),
                new User("Laura", "laura@empresa.com", "Finanzas"), IncidentPriority.P2), id);
    }

    static AiQuery query(long id, Incident incident, String createdAt) {
        AiQuery query = withId(new AiQuery(incident, "¿Por qué no hay internet?"), id);
        ReflectionTestUtils.setField(query, "createdAt", Instant.parse(createdAt));
        return query;
    }

    static KnowledgeArticle article(long id, String title) {
        return withId(new KnowledgeArticle(title, "problem", "symptoms", "cause", "solution"), id);
    }

    static AiResponse response(long id, AiQuery query, String answerJson) {
        return withId(new AiResponse(query, answerJson), id);
    }

    static Rating rating(long id, AiResponse response, int score, String comment) {
        return withId(new Rating(response, score, comment), id);
    }
}
