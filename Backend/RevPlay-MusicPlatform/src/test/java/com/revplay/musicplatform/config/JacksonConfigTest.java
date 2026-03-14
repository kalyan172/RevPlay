package com.revplay.musicplatform.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Tag("unit")
class JacksonConfigTest {

    @Test
    @DisplayName("object mapper disables timestamp serialization for dates")
    void objectMapperDisablesDateTimestamps() {
        JacksonConfig jacksonConfig = new JacksonConfig();
        ObjectMapper objectMapper = jacksonConfig.objectMapper(new Jackson2ObjectMapperBuilder());

        assertThat(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    }

    @Test
    @DisplayName("object mapper serializes LocalDateTime as ISO string")
    void objectMapperSerializesJavaTimeAsIso() throws Exception {
        JacksonConfig jacksonConfig = new JacksonConfig();
        ObjectMapper objectMapper = jacksonConfig.objectMapper(new Jackson2ObjectMapperBuilder());
        LocalDateTime value = LocalDateTime.of(2026, 3, 9, 3, 0, 0);

        String json = objectMapper.writeValueAsString(value);

        assertThat(json).contains("2026-03-09T03:00:00");
    }
}
