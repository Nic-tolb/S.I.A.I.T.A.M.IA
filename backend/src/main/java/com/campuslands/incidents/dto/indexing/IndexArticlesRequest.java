package com.campuslands.incidents.dto.indexing;

import java.util.List;

/**
 * Body sent to the n8n indexing webhook (contract in n8n/README.md).
 */
public record IndexArticlesRequest(List<ArticleIndexPayload> articles) {

    public IndexArticlesRequest {
        articles = articles == null ? List.of() : List.copyOf(articles);
    }
}
