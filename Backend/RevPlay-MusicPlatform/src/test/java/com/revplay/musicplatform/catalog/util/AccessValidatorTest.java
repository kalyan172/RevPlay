package com.revplay.musicplatform.catalog.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class AccessValidatorTest {

    private final AccessValidator validator = new AccessValidator();

    @Test
    @DisplayName("requireArtistOrAdmin accepts artist and admin roles")
    void requireArtistOrAdminAccepts() {
        assertThatCode(() -> validator.requireArtistOrAdmin("ARTIST")).doesNotThrowAnyException();
        assertThatCode(() -> validator.requireArtistOrAdmin("ROLE_ADMIN")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("requireArtistOrAdmin rejects listener role")
    void requireArtistOrAdminRejectsListener() {
        assertThatThrownBy(() -> validator.requireArtistOrAdmin("LISTENER"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Access denied");
    }

    @Test
    @DisplayName("requireAdmin accepts only admin")
    void requireAdminBehavior() {
        assertThatCode(() -> validator.requireAdmin("admin")).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.requireAdmin("ARTIST"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Access denied");
    }
}
