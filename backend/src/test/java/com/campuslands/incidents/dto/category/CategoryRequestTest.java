package com.campuslands.incidents.dto.category;

import static com.campuslands.incidents.dto.ValidationTestSupport.invalidFields;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoryRequestTest {

    @Test
    void descriptionIsOptional() {
        var request = new CategoryRequest("Conectividad", null);

        assertThat(invalidFields(request)).isEmpty();
    }

    @Test
    void blankNameIsRejected() {
        var request = new CategoryRequest("", "Redes e internet");

        assertThat(invalidFields(request)).containsExactly("name");
    }

    @Test
    void tooLongNameIsRejected() {
        var request = new CategoryRequest("x".repeat(81), null);

        assertThat(invalidFields(request)).containsExactly("name");
    }
}
