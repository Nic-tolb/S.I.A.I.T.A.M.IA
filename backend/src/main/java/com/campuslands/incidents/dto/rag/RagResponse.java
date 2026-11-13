package com.campuslands.incidents.dto.rag;

import java.util.List;

import com.campuslands.incidents.dto.assistance.AssistanceAnswer;
import com.campuslands.incidents.dto.assistance.AssistanceSource;

/**
 * Body returned by the n8n RAG webhook (contract in n8n/README.md).
 *
 * @param status {@code ANSWERED}, {@code NO_RESULTS} or {@code ERROR}, kept as text so an
 *               unexpected value is handled as an error instead of failing to parse
 */
public record RagResponse(String status, AssistanceAnswer answer, List<AssistanceSource> sources, String error) {

    public RagResponse {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
