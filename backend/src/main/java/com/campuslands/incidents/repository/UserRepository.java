package com.campuslands.incidents.repository;

import com.campuslands.incidents.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
