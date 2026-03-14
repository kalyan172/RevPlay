package com.revplay.musicplatform.user.util;

import java.security.SecureRandom;

public final class OtpGeneratorUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private OtpGeneratorUtil() {
    }

    public static String generateOtp() {
        int value = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}

