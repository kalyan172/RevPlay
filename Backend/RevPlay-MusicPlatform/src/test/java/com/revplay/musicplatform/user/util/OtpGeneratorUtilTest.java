package com.revplay.musicplatform.user.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class OtpGeneratorUtilTest {

    @Test
    @DisplayName("generateOtp returns six digit numeric string")
    void generateOtpFormat() {
        String otp = OtpGeneratorUtil.generateOtp();

        assertThat(otp).matches("\\d{6}");
    }

    @Test
    @DisplayName("generateOtp is not constant across multiple calls")
    void generateOtpNotConstant() {
        java.util.Set<String> values = new java.util.HashSet<>();
        for (int i = 0; i < 20; i++) {
            values.add(OtpGeneratorUtil.generateOtp());
        }

        assertThat(values.size()).isGreaterThan(1);
    }
}
