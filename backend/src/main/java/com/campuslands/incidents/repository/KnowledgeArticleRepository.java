package com.campuslands.incidents.repository;

import com.campuslands.incidents.entity.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {
}
