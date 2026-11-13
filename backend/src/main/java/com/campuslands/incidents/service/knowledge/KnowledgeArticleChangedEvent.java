package com.campuslands.incidents.service.knowledge;

import com.campuslands.incidents.dto.indexing.ArticleIndexPayload;

/**
 * Published by the knowledge base service when an article is created, updated, activated or
 * deactivated, so the vector store can be synced after the transaction commits.
 *
 * @param payload built inside the transaction with {@link ArticleIndexPayload#from}, so the
 *                listener does not need to read the article again; its {@code active} flag is
 *                true only for articles that are active and verified
 */
public record KnowledgeArticleChangedEvent(ArticleIndexPayload payload) {
}
