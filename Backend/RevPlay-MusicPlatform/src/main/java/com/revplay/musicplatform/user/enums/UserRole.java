package com.revplay.musicplatform.user.enums;

import com.revplay.musicplatform.user.exception.AuthValidationException;

public enum UserRole {
    LISTENER,
    ARTIST,
    ADMIN;

    public static UserRole from(String value) {
        if (value == null || value.isBlank()) {
            throw new AuthValidationException("role is required");
        }
        try {
            return UserRole.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new AuthValidationException("role must be one of: LISTENER, ARTIST, ADMIN");
        }
    }
}
