package com.campuslands.incidents.repository;

import com.campuslands.incidents.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Used on create: users.email is UNIQUE, so a duplicate is reported as 409 instead of failing in the DB. */
    boolean existsByEmailIgnoreCase(String email);

    /** Used on update: same check, ignoring the user being edited. */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
