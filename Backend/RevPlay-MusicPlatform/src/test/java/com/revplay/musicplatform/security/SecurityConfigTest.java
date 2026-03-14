package com.revplay.musicplatform.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Tag("unit")
class SecurityConfigTest {

    private static final String LOCALHOST_ORIGIN = "http://localhost:4200";
    private static final String AUTHORIZATION = "Authorization";
    private static final String CONTENT_TYPE = "Content-Type";

    @Test
    @DisplayName("corsConfigurationSource registers expected cors settings")
    void corsConfigurationSourceRegistersExpectedSettings() {
        SecurityConfig securityConfig = new SecurityConfig(null);

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);
        CorsConfiguration config = source.getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/test"));
        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactly(LOCALHOST_ORIGIN);
        assertThat(config.getAllowedMethods()).containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(config.getAllowedHeaders()).contains(AUTHORIZATION, CONTENT_TYPE);
        assertThat(config.getExposedHeaders()).contains(AUTHORIZATION, CONTENT_TYPE);
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("passwordEncoder returns bcrypt implementation")
    void passwordEncoderReturnsBcrypt() {
        SecurityConfig securityConfig = new SecurityConfig(null);

        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
        String encoded = passwordEncoder.encode("password-123");
        assertThat(passwordEncoder.matches("password-123", encoded)).isTrue();
    }
}
