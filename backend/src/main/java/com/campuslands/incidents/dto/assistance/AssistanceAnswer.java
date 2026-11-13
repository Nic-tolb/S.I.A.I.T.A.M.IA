package com.campuslands.incidents.dto.assistance;

import java.util.List;

/**
 * Answer written by the model from the knowledge articles (user-facing text, in Spanish).
 * Same shape as {@code answer} in the n8n contract (n8n/README.md).
 */
public record AssistanceAnswer(String solution, List<String> steps) {

    public AssistanceAnswer {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
