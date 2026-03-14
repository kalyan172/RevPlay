package com.revplay.musicplatform.catalog.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.catalog.exception.DiscoveryValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class DiscoveryValidationUtilTest {

    @Test
    @DisplayName("requirePositiveId throws for null or non-positive values")
    void requirePositiveIdInvalid() {
        assertThatThrownBy(() -> DiscoveryValidationUtil.requirePositiveId(null, "artistId"))
                .isInstanceOf(DiscoveryValidationException.class);
        assertThatThrownBy(() -> DiscoveryValidationUtil.requirePositiveId(0L, "artistId"))
                .isInstanceOf(DiscoveryValidationException.class);
    }

    @Test
    @DisplayName("requirePositiveId allows positive value")
    void requirePositiveIdValid() {
        assertThatCode(() -> DiscoveryValidationUtil.requirePositiveId(1L, "artistId"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("requirePageSize validates page and size boundaries")
    void requirePageSizeValidation() {
        assertThatThrownBy(() -> DiscoveryValidationUtil.requirePageSize(-1, 10))
                .isInstanceOf(DiscoveryValidationException.class);
        assertThatThrownBy(() -> DiscoveryValidationUtil.requirePageSize(0, 101))
                .isInstanceOf(DiscoveryValidationException.class);
    }

    @Test
    @DisplayName("safeOffset multiplies page and size")
    void safeOffsetComputes() {
        assertThat(DiscoveryValidationUtil.safeOffset(2, 25)).isEqualTo(50);
    }

    @Test
    @DisplayName("requireNotBlank rejects blank and accepts non-blank")
    void requireNotBlankBehavior() {
        assertThatThrownBy(() -> DiscoveryValidationUtil.requireNotBlank(" ", "q"))
                .isInstanceOf(DiscoveryValidationException.class);
        assertThatCode(() -> DiscoveryValidationUtil.requireNotBlank("rock", "q"))
                .doesNotThrowAnyException();
    }
}
