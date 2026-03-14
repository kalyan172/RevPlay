package com.revplay.musicplatform.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class JwtPropertiesTest {

    @Test
    @DisplayName("getters and setters retain configured values")
    void gettersSetters() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("secret");
        properties.setAccessTokenExpirationSeconds(3600L);
        properties.setRefreshTokenExpirationSeconds(7200L);

        assertThat(properties.getSecret()).isEqualTo("secret");
        assertThat(properties.getAccessTokenExpirationSeconds()).isEqualTo(3600L);
        assertThat(properties.getRefreshTokenExpirationSeconds()).isEqualTo(7200L);
    }
}
