package com.campuslands.incidents.dto.user;

import static com.campuslands.incidents.dto.ValidationTestSupport.invalidFields;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserRequestTest {

    @Test
    void validUserHasNoViolations() {
        var request = new UserRequest("Laura Gómez", "laura.gomez@empresa.com", "Finanzas");

        assertThat(invalidFields(request)).isEmpty();
    }

    @Test
    void blankFieldsAreRejected() {
        var request = new UserRequest(" ", "", null);

        assertThat(invalidFields(request)).containsExactlyInAnyOrder("name", "email", "area");
    }

    @Test
    void malformedEmailIsRejected() {
        var request = new UserRequest("Laura Gómez", "laura-at-empresa", "Finanzas");

        assertThat(invalidFields(request)).containsExactly("email");
    }
}
