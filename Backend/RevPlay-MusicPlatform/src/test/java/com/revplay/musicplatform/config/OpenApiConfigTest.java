package com.revplay.musicplatform.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class OpenApiConfigTest {

    private static final String SECURITY_NAME = "Bearer Authentication";
    private static final String TITLE = "RevPlay API";
    private static final String VERSION = "1.0";
    private static final String DESCRIPTION = "API Documentation for RevPlay Music Streaming Service";
    private static final String BEARER = "bearer";
    private static final String JWT = "JWT";

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    @DisplayName("customOpenAPI configures info metadata")
    void customOpenApiConfiguresInfoMetadata() {
        OpenAPI openApi = openApiConfig.customOpenAPI();

        assertThat(openApi.getInfo()).isNotNull();
        assertThat(openApi.getInfo().getTitle()).isEqualTo(TITLE);
        assertThat(openApi.getInfo().getVersion()).isEqualTo(VERSION);
        assertThat(openApi.getInfo().getDescription()).isEqualTo(DESCRIPTION);
    }

    @Test
    @DisplayName("customOpenAPI configures bearer jwt security scheme")
    void customOpenApiConfiguresBearerScheme() {
        OpenAPI openApi = openApiConfig.customOpenAPI();
        SecurityScheme securityScheme = openApi.getComponents().getSecuritySchemes().get(SECURITY_NAME);

        assertThat(securityScheme).isNotNull();
        assertThat(securityScheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(securityScheme.getScheme()).isEqualTo(BEARER);
        assertThat(securityScheme.getBearerFormat()).isEqualTo(JWT);
        assertThat(openApi.getSecurity()).isNotEmpty();
    }
}
