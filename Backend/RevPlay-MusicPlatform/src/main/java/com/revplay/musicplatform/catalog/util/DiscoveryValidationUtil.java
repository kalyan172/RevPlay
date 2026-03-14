package com.revplay.musicplatform.catalog.util;

import com.revplay.musicplatform.catalog.exception.DiscoveryValidationException;

public final class DiscoveryValidationUtil {

    private DiscoveryValidationUtil() {
    }

    public static void requirePositiveId(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new DiscoveryValidationException(fieldName + " must be a positive number");
        }
    }

    public static void requirePageSize(int page, int size) {
        if (page < 0) {
            throw new DiscoveryValidationException("page must be 0 or greater");
        }
        if (size < 1 || size > 100) {
            throw new DiscoveryValidationException("size must be between 1 and 100");
        }
    }

    public static int safeOffset(int page, int size) {
        return page * size;
    }

    public static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new DiscoveryValidationException(fieldName + " is required");
        }
    }
}


