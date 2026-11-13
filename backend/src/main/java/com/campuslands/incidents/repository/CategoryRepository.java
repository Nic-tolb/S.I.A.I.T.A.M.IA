package com.campuslands.incidents.repository;

import com.campuslands.incidents.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Used on create: categories.name is UNIQUE, so a duplicate is reported as 409 instead of failing in the DB. */
    boolean existsByNameIgnoreCase(String name);

    /** Used on update: same check, ignoring the category being edited. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
