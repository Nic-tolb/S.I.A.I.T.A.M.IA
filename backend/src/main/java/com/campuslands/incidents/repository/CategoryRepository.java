package com.campuslands.incidents.repository;

import com.campuslands.incidents.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
