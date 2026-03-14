package com.revplay.musicplatform.user.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.user.exception.AuthValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class UserRoleTest {

    @Test
    @DisplayName("from resolves case insensitive valid values")
    void fromValid() {
        assertThat(UserRole.from("listener")).isEqualTo(UserRole.LISTENER);
        assertThat(UserRole.from("ARTIST")).isEqualTo(UserRole.ARTIST);
        assertThat(UserRole.from("admin")).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("from throws validation error for null or blank")
    void fromNullOrBlank() {
        assertThatThrownBy(() -> UserRole.from(null))
                .isInstanceOf(AuthValidationException.class)
                .hasMessage("role is required");
        assertThatThrownBy(() -> UserRole.from("   "))
                .isInstanceOf(AuthValidationException.class)
                .hasMessage("role is required");
    }

    @Test
    @DisplayName("from throws validation error for unknown role")
    void fromInvalid() {
        assertThatThrownBy(() -> UserRole.from("manager"))
                .isInstanceOf(AuthValidationException.class)
                .hasMessage("role must be one of: LISTENER, ARTIST, ADMIN");
    }
}
