package com.campuslands.incidents.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for creating or updating a category. The description is optional.
 */
public record CategoryRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String name,

        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String description) {
}
