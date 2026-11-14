package com.campuslands.incidents.dto.history;

import static com.campuslands.incidents.dto.ValidationTestSupport.invalidFields;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RatingRequestTest {

    @Test
    void scoreBetweenOneAndFiveWithOptionalCommentIsValid() {
        assertThat(invalidFields(new RatingRequest(5, null))).isEmpty();
        assertThat(invalidFields(new RatingRequest(1, "Resolvió el problema"))).isEmpty();
    }

    @Test
    void scoreOutsideOneToFiveIsRejected() {
        assertThat(invalidFields(new RatingRequest(0, null))).containsExactly("score");
        assertThat(invalidFields(new RatingRequest(6, null))).containsExactly("score");
    }

    @Test
    void missingScoreAndTooLongCommentAreRejected() {
        assertThat(invalidFields(new RatingRequest(null, "x".repeat(501))))
                .containsExactlyInAnyOrder("score", "comment");
    }
}
