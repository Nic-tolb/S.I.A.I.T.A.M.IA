package com.campuslands.incidents.dto.user;

public record UserResponse(
        Long id,
        String name,
        String email,
        String area) {
}
