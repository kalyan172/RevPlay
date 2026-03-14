package com.revplay.musicplatform.playback.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ValidQueueContentSelectionTest {

    private static final String DEFAULT_MESSAGE = "Exactly one of songId or episodeId must be provided";

    @Test
    @DisplayName("annotation default message and validator type are configured")
    void annotationDefaultsAreConfigured() throws Exception {
        Object defaultMessage = ValidQueueContentSelection.class.getMethod("message").getDefaultValue();
        Annotation constraintAnnotation = ValidQueueContentSelection.class.getAnnotation(jakarta.validation.Constraint.class);

        assertThat(defaultMessage).isEqualTo(DEFAULT_MESSAGE);
        assertThat(constraintAnnotation).isNotNull();
        assertThat(((jakarta.validation.Constraint) constraintAnnotation).validatedBy())
                .containsExactly(QueueContentSelectionValidator.class);
    }
}
